
public class Point {
	double x;
	double y;
	
	public Point(double x, double y){
		this.x = x;
		this.y = y;
	}
	
	public double gradient_with(Point p){
		return (p.y - this.y) / (p.x - this.x);
	}
	
	public boolean equals(Point p){
		boolean ret = false;
		double abs_x = Math.abs(this.x - p.x);
		double abs_y = Math.abs(this.y - p.y);
		if (abs_x < 0.0000000001)
			if (abs_y < 0.0000000001)
				ret = true;
		return ret;
	}
	
}
