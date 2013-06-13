

the-lift: the-lift.o


the-lift.o: the-lift.cpp


PROTOBUF_CFLAGS = `pkg-config --cflags protobuf`
PROTOBUF_LFLAGS = `pkg-config --libs protobuf`
BOOST_LFLAGS = -lboost_system


the-lift: the-lift.o stringdb.pb.o 
	g++ -o the-lift the-lift.o db_server.o stringdb.pb.o \
                $(PROTOBUF_LFLAGS) $(BOOST_LFLAGS)

stringdb.pb.o: stringdb.proto
	protoc --cpp_out=. --python_out=. stringdb.proto
	g++ -c stringdb.pb.cc $(PROTOBUF_CFLAGS)

HEADERS = $(wildcard *.h)

%.o: %.cpp stringdb.pb.o ${HEADERS}
	g++ -c $<

clean:
	rm -f *.pb.*
	rm -f *_pb2.*
	rm -f *.o 
	rm -f the-lift 


