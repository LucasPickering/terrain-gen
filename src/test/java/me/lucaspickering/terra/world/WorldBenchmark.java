package me.lucaspickering.terra.world;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Threads(1)
@Warmup(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(value = 5)
@BenchmarkMode(Mode.AverageTime)
public class WorldBenchmark {

    private final long seed = "this is a great seed".hashCode();

    @Param({"5", "10"})
    private int size;

    @Benchmark
    public void measureGenerate() {
        final WorldHandler worldHandler = new WorldHandler(seed, size);
        worldHandler.generate();
    }
}