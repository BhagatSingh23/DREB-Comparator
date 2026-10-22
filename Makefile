# DREB Comparator Benchmark Framework
JAVA_HOME ?= /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
CONFIG ?= config/smoke_experiment.yaml
OUTPUT_DIR ?= results/latest

.PHONY: all compile test verify smoke reproduce full-reproduce clean

all: test

compile:
	JAVA_HOME=$(JAVA_HOME) mvn compile test-compile

test:
	JAVA_HOME=$(JAVA_HOME) mvn test

verify:
	JAVA_HOME=$(JAVA_HOME) mvn verify

# Smoke target: quick 2-seed validation run (< 10 seconds)
smoke:
	JAVA_HOME=$(JAVA_HOME) mvn exec:java \
		-Dexec.mainClass="com.ratelimiter.benchmark.BenchmarkRunner" \
		-Dexec.args="--config config/smoke_experiment.yaml --output-dir results/smoke"

# Reproduce target: regenerates all CSVs, tables and figures from scratch
reproduce: compile
	JAVA_HOME=$(JAVA_HOME) mvn exec:java \
		-Dexec.mainClass="com.ratelimiter.benchmark.BenchmarkRunner" \
		-Dexec.args="--config $(CONFIG) --output-dir $(OUTPUT_DIR)"

full-reproduce: compile
	JAVA_HOME=$(JAVA_HOME) mvn exec:java \
		-Dexec.mainClass="com.ratelimiter.benchmark.BenchmarkRunner" \
		-Dexec.args="--config config/experiments.yaml --output-dir $(OUTPUT_DIR)"

clean:
	JAVA_HOME=$(JAVA_HOME) mvn clean
