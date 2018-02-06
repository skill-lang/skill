package simpleEmpty;

public class Simple {

    public Simple()  { };
	
	public long x;
	
	public long y;
	
	public byte b;
	
	public short s;
	
	public int i;
	
	public long l;
	
	public float f;
	
	public double d;
	
	public String str;
	
	public Simple(byte b, short s, int i, long l, float f, double d, String str) {
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
