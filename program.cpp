
int main()
{
	CLASS a = new CLASS;
	a.c = 0;
	println(toString(a.c));
	a.a = a;
	println(toString(a.a.a.a.a.a.a.a.a.a.a.c));
	CLASS b = new CLASS;
	b.a = a;
	b.b = b;
	a.b = b;
	println(toString(a.b.a.b.a.b.a.c));
	return 0;
}

class CLASS
{
	CLASS a;
	CLASS b;
	int c;
}



/*!! metadata:
=== comment ===
5140309234-xietiancheng/class.txt
=== assert ===
success_compile
=== phase ===
semantic pretest
=== is_public ===
True

!!*/

