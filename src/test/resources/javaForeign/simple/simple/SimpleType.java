package simple;

public class SimpleType {
	
	long x;
	
	long y;
	
	byte b;
	
	short s;
	
	int i;
	
	long l;
	
	float f;
	
	double d;
	
	String str;
	
	public SimpleType(byte b, short s, int i, long l, float f, double d, String str) {
		super();
		this.b = b;
		this.s = s;
		this.i = i;
		this.l = l;
		this.f = f;
		this.d = d;
		this.str = str;
	}

	@Override
	public String toString() {
		return "SimpleType [b=" + b + ", s=" + s + ", i=" + i + ", l=" + l + ", f=" + f + ", d=" + d + ", str=" + str
				+ "]";
	}
}
