@echo off 

REM -XX:+PrintGCDetails
REM -Xloggc:jworm-gc.log

set XMX=-Xms512M -Xloggc:jworm-gc.log

set AGENT=-javaagent:bin/deuceAgent.jar
set DELEGATOR=-Dorg.deuce.delegator=org.deuce.transaction.jvstm.ContextDelegator
set CAPMEM=-Djvstm.capmem=true
set STM_CTX=-Dorg.deuce.transaction.contextClass=org.deuce.transaction.jvstm.Context
set EXCL=-Dorg.deuce.exclude=jwormbench.defaults.World,jwormbench.core.IWorld,java.lang.Integer,jvstm.*,java.lang.System
set LOG=-Djava.util.logging.config.file=config/logging-tests.properties
set AOM=-Djvstm.aom.reversion=false
set RO=-Dorg.deuce.transaction.jvstm.rohint=true
set CP=-classpath bin\deuceTests.jar;lib\jwormbench.jar;lib\jvstm-2.0.jar

REM arg 0: 512 iterations
REM arg 1: 4 nr of threads
REM arg 2: 22 workload
REM arg 3: deuce synchronization technique
REM arg 4: 120 timeout

REM set ARGS=512 4 22 deuce
set ARGS=8192 4 22 deuce 240

set SYS_PROPS=%XMX% %AGENT% %DELEGATOR% %CAPMEM% %STM_CTX% %EXCL% %LOG% %AOM% %RO% %CP%

java %SYS_PROPS% org.deuce.benchmark.jwormbench.RunJWormBench %ARGS%