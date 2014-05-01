@echo off 

set XBOOT=-Xbootclasspath:lib/rt-deuce-jvstm.jar;bin/deuceAgent.jar;lib/jvstm-2.0.jar
REM -Xmx1472M 
set XMX=-Xloggc:bench7-gc.log
set AGENT=-javaagent:bin/deuceAgent.jar
set DELEGATOR=-Dorg.deuce.delegator=org.deuce.transaction.jvstm.ContextDelegator
set CAPMEM=-Djvstm.capmem=false
set STM_CTX=-Dorg.deuce.transaction.contextClass=org.deuce.transaction.jvstm.Context
set EXCL=-Dorg.deuce.exclude=java.lang.String,org.deuce.benchmark.stmbench7.ThreadRandom,java.lang.System,sun.reflect.*,java.lang.StringBuilder
set LOG=-Djava.util.logging.config.file=config/logging-tests.properties
set AOM=-Djvstm.aom.reversion=true
set RO= -Dorg.deuce.transaction.jvstm.rohint=true
set CP=-classpath bin\deuceTests.jar;lib\jwormbench.jar;lib\jvstm-2.0.jar
set BENCH_ARGS=-g stm -s org.deuce.benchmark.stmbench7.impl.deucestm.DeuceSTMInitializer -t 1 -w r --no-traversals --no-sms -l 120

set SYS_PROPS=%XBOOT% %XMX% %AGENT% %DELEGATOR% %CAPMEM% %STM_CTX% %EXCL% %LOG% %AOM% %RO% %CP%

java %SYS_PROPS% org.deuce.benchmark.stmbench7.Benchmark %BENCH_ARGS%