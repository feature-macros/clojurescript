.PHONY: deps demo

bin/boot:
	mkdir -p bin
	wget -O bin/boot https://github.com/boot-clj/boot/releases/download/2.0.0-rc8/boot.sh
	chmod 0755 bin/boot

../pom.xml:
	(cd ../ && ./script/bootstrap && ./script/clean && ./script/build)

.deps.time: bin/boot ../pom.xml
	touch .deps.time

deps: .deps.time

demo:
	./bin/boot demo
