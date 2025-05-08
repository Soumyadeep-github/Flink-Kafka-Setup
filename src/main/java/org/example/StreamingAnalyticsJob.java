package org.example;

import java.time.Duration;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.Partitioner;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.CheckpointingOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.StateBackendOptions;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.contrib.streaming.state.EmbeddedRocksDBStateBackend;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.datastream.ConnectedStreams;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.CoMapFunction;
import org.apache.flink.streaming.api.functions.co.ProcessJoinFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.example.models.Transaction;
import org.example.models.UserActivity;

public class StreamingAnalyticsJob {

    private static Configuration getConfiguration() {
        Configuration configuration = new Configuration();
        configuration.set(StateBackendOptions.STATE_BACKEND, "rocksdb");
        configuration.set(CheckpointingOptions.CHECKPOINT_STORAGE, "filesystem");
        configuration.set(CheckpointingOptions.CHECKPOINTS_DIRECTORY, "file:///tmp/flink-rocksdb-checkpoints");
        return configuration;
    }

    public static void main(String[] args) throws Exception {
        Configuration configuration = getConfiguration();
        // Set up the streaming execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.configure(configuration);
        env.enableChangelogStateBackend(true);
        // Enable checkpointing and set state backend
//        env.enableCheckpointing(60000);
//        env.setStateBackend(new FsStateBackend("file:///tmp/flink-checkpoints"));
//
//        // Configure RocksDB state backend
//        EmbeddedRocksDBStateBackend stateBackend = new EmbeddedRocksDBStateBackend();
//        stateBackend.setDbStoragePath("file:///tmp/flink-rocksdb"); // Optional: Set storage path
//        env.setStateBackend(stateBackend);


        // Configure Kafka sources
        String kafkaBootstrapServers = "localhost:9093";
        String consumerGroupId = "flink-analytics";

        // Configure Kafka sources using KafkaSource
        KafkaSource<String> userActivitySource = KafkaSource.<String>builder()
                .setBootstrapServers(kafkaBootstrapServers)
                .setTopics("user_activities")
                .setGroupId(consumerGroupId)
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        KafkaSource<String> transactionSource = KafkaSource.<String>builder()
                .setBootstrapServers(kafkaBootstrapServers)
                .setTopics("transactions")
                .setGroupId(consumerGroupId)
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();


        // Use fromSource instead of addSource
        DataStream<UserActivity> userActivityStream = env
                .fromSource(userActivitySource, WatermarkStrategy.noWatermarks(), "UserActivitySource")
                .map(json -> new ObjectMapper().readValue(json, UserActivity.class));

        DataStream<Transaction> transactionStream = env
                .fromSource(transactionSource, WatermarkStrategy.noWatermarks(), "TransactionSource")
                .map(json -> new ObjectMapper().readValue(json, Transaction.class));


        // 1. Window Operations
        DataStream<Tuple2<String, Double>> windowedTransactions = transactionStream
            .keyBy(Transaction::getUserId)
            .window(TumblingEventTimeWindows.of(Time.minutes(5)))
            .aggregate(new AggregateFunction<Transaction, Tuple2<String, Double>, Tuple2<String, Double>>() {
                @Override
                public Tuple2<String, Double> createAccumulator() {
                    return new Tuple2<>("", 0.0);
                }

                @Override
                public Tuple2<String, Double> add(Transaction value, Tuple2<String, Double> acc) {
                    return new Tuple2<>(value.getUserId(), acc.f1 + value.getAmount());
                }

                @Override
                public Tuple2<String, Double> getResult(Tuple2<String, Double> acc) {
                    return acc;
                }

                @Override
                public Tuple2<String, Double> merge(Tuple2<String, Double> a, Tuple2<String, Double> b) {
                    return new Tuple2<>(a.f0, a.f1 + b.f1);
                }
            })
            .name("WindowedTransactions");

        // 2. Window Join
        DataStream<Tuple3<String, String, Double>> joinedStream = userActivityStream
                .keyBy(UserActivity::getUserId)
                .intervalJoin(transactionStream.keyBy(Transaction::getUserId))
                .between(Time.minutes(-5), Time.minutes(5)) // Define the join window
                .process(new ProcessJoinFunction<UserActivity, Transaction, Tuple3<String, String, Double>>() {
                    @Override
                    public void processElement(UserActivity activity, Transaction transaction, Context ctx, Collector<Tuple3<String, String, Double>> out) {
                        out.collect(new Tuple3<>(activity.getUserId(), activity.getActivityType(), transaction.getAmount()));
                    }
                })
                .name("WindowJoin");

        // 3. Connect and CoMap
        ConnectedStreams<UserActivity, Transaction> connectedStreams = userActivityStream
            .connect(transactionStream)
            .keyBy(UserActivity::getUserId, Transaction::getUserId);

        DataStream<String> connectedResult = connectedStreams
            .map(new CoMapFunction<UserActivity, Transaction, String>() {
                @Override
                public String map1(UserActivity activity) {
                    return "Activity: " + activity.getActivityType();
                }

                @Override
                public String map2(Transaction transaction) {
                    return "Transaction: " + transaction.getAmount();
                }
            })
            .name("ConnectedStream");

        // 4. Partitioning
        DataStream<UserActivity> partitionedStream = userActivityStream
            .partitionCustom(new Partitioner<String>() {
                @Override
                public int partition(String key, int numPartitions) {
                    return Math.abs(key.hashCode() % numPartitions);
                }
            }, UserActivity::getUserId);
//            .name("PartitionedStream");

//        DataStream<UserActivity> partitionedStream = userActivityStream
//                .keyBy(UserActivity::getUserId)
//                .process(new ProcessFunction<UserActivity, UserActivity>() {
//                    @Override
//                    public void processElement(UserActivity value, Context ctx, Collector<UserActivity> out) {
//                        int numPartitions = ctx.getExecutionConfig().getParallelism();
//                        int numPartitions = getRuntimeContext().getNumberOfParallelSubtasks();
//                        int partition = Math.abs(value.getUserId().hashCode() % numPartitions);
//                        // Custom partitioning logic can be applied here if needed
//                        out.collect(value); // Emit the element
//                    }
//                })
//                .name("PartitionedStream");

        // 5. Window Partition
        DataStream<Tuple2<String, Integer>> windowPartitioned = userActivityStream
            .keyBy(UserActivity::getActivityType)
            .window(TumblingEventTimeWindows.of(Time.minutes(5)))
            .process(new ProcessWindowFunction<UserActivity, Tuple2<String, Integer>, String, TimeWindow>() {
                @Override
                public void process(String key, Context ctx, Iterable<UserActivity> elements, 
                                  Collector<Tuple2<String, Integer>> out) {
                    int count = 0;
                    for (UserActivity activity : elements) {
                        count++;
                    }
                    out.collect(new Tuple2<>(key, count));
                }
            })
            .name("WindowPartition");

        // 6. Task Chaining
//        DataStream<Tuple2<String, Double>> chainedStream = transactionStream
//            .map(t -> new Tuple2<>(t.getUserId(), t.getAmount()))
//            .filter(t -> t.f1 > 100.0)
//            .map(t -> new Tuple2<>(t.f0, t.f1 * 1.1))
//            .name("ChainedStream");
        DataStream<Tuple2<String, Double>> chainedStream = transactionStream
                .map(t -> new Tuple2<>(t.getUserId(), t.getAmount()))
                .returns(TypeInformation.of(new TypeHint<Tuple2<String, Double>>() {})) // Specify return type
                .filter(t -> t.f1 > 100.0)
                .returns(TypeInformation.of(new TypeHint<Tuple2<String, Double>>() {})) // Specify return type
                .map(t -> new Tuple2<>(t.f0, t.f1 * 1.1))
                .returns(TypeInformation.of(new TypeHint<Tuple2<String, Double>>() {})) // Specify return type
                .name("ChainedStream");

        // Add sinks (for demonstration, we'll just print)
        windowedTransactions.print("Windowed Transactions");
        joinedStream.print("Joined Stream");
        connectedResult.print("Connected Result");
        partitionedStream.print("Partitioned Stream");
        windowPartitioned.print("Window Partitioned");
        chainedStream.print("Chained Stream");

        // Execute the job
        env.execute("Streaming Analytics Job");
    }


}
