all: build

build: 
	./bin/compile.sh

clean: 
	mvn clean

example:
	tornado uk.ac.manchester.tornado.examples.HelloWorld

tests:
	tornado-test.py --verbose
	test-native.sh 

test-slam:
	tornado-test.py -V --fast uk.ac.manchester.tornado.unittests.slam.graphics.GraphicsTests 

eclipse:
	mvn eclipse:eclipse

clean-graphs:
	rm *.cfg *.bgv *.log

