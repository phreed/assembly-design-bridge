#include <iostream>

class foo {
	public:
void fun1( ) {
	std::cout << "0 arg" << std::endl;
}
void fun1( int x ) {
	std::cout << "1 arg" << std::endl;
}

void fun1( int x, int y ) {
	std::cout << "2 args" << std::endl;
}
};

int main( void ) {
	void (foo::*v)();
	void (foo::*x)(int);
	void (foo::*y)(int, int);

	foo foo1;

	v = &foo::fun1;
	x = &foo::fun1;
	y = &foo::fun1;

	(foo1.*(v))();
	(foo1.*(x))(1);
	(foo1.*(y))(1,2);
}
