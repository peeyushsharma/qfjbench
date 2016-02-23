package org.qfjbench;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import quickfix.Message;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@State(Scope.Thread)
public class QFJMessageParsingBenchmark {
	
	public final FIXUtil parser = new FIXUtil();
	
	final String messageString = "8=FIX.4.0\0019=56\00135=A\00134=1\00149=TW\001" +
            "52=20060118-16:34:19\00156=ISLD\00198=0\001108=2\00110=223\001";
	
	final String messageNewOrderSingle = "8=FIX.4.3\0019=238\00135=D\00134=324\00149=order.BARONSUCD.006\00152=20160119-00:40:21.431\00156=prod.fxgrid\00111=143804:18950865:19790744:86004\00115=GBP\00118=B\00121=1\00138=67000\00140=1\00154=1\00155=GBP/USD\00159=6\00160=20160119-00:40:21.431\001110=0\001126=19700101-00:00:02\001167=FOR\001210=67000\001460=4\00110=251\001";
	
	Message nos;
	Message logon;
	
	@Setup
	public void setup() {
		parser.init();
		nos = parser.deserialize(messageNewOrderSingle);
		logon = parser.deserialize(messageString);
	}
	
	@Benchmark
	public void deserializeLogon()
	{
		parser.deserialize(messageString);
	}

	@Benchmark
	public void deserializeNewOrderSingle()
	{
		parser.deserialize(messageNewOrderSingle);
	}

	@Benchmark
	public void serializeLogon()
	{
		parser.serialize(logon);
	}

	@Benchmark
	public void serializeNewOrderSingle()
	{
		parser.serialize(nos);
	}
	
	
}
