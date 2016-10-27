import java.util.LinkedList;


public class LinearFunction {
	
	// f(x) = ax + b
	double a;
	double b;
	
	LinkedList<Point> points;
	LinkedList<LinearFunction> functions;
	
//	Point p = new Point(0.5, 0.0);
//	double[] grad    = {	1.0,	5.0,	25.0,	100.0 };
//	double[] breadth = {	0.2,	0.15,	0.1,	0.05};
	
	Point p = new Point(0.6, 0.0);
	double[] grad    = {	1.0,	2.0,	4.0,	8.0,	16.0,	32.0,	64.0,	128.0};
	double[] breadth = {	0.05,	0.05,	0.05,	0.05,	0.05,	0.05,	0.05,	0.05};

	
	public LinearFunction(){
		this.make_functions_from_a_single_point_and_multiple_gradients();
	}
	
	public LinearFunction(LinkedList<Point> pointlist){
		this.make_functions_from_point_pairs(pointlist);
	}
	
	public LinearFunction(Point s, double[] br, double[] gr){
		this.p = s;
		this.breadth = br;
		this.grad = gr;
		this.make_functions_from_a_single_point_and_multiple_gradients();
	}
	
	public LinearFunction(Point s, Point t){
		this.a = s.gradient_with(t);
		this.b = s.y - a*s.x;
	}
	
	public LinearFunction(Point s, double grad){
		this.a = grad;
		this.b = s.y - a*s.x;
	}
	
	
	// get the exact cost value for a specific utilization value
	public double calculate(double utilization){
		double cost = -10000000000.0;
		for (LinearFunction func : this.functions)
			if (cost < func.a*utilization + func.b)
				cost = func.a*utilization + func.b;
		return cost;
	}
	
	
	public void make_functions_from_points(LinkedList<Point> points){
		this.points = points;
		this.functions = new LinkedList<LinearFunction>();
		for (int i=1; i<points.size(); i++){
			LinearFunction f = new LinearFunction(points.get(i-1), points.get(i));
			this.functions.addLast(f);
		}
	}
	
	// fewer functions to reduce number of constraints
	public void make_functions_from_point_pairs(LinkedList<Point> points_list){
		if (points_list.size()%2 != 0){
			System.out.println("WARNING! No cost functions where defined. Please provide an even number of points.");
			return;
		}
		this.points = points_list;
		LinkedList<Point> deletelist = new LinkedList<Point>();
		deletelist.addAll(points_list);
		this.functions = new LinkedList<LinearFunction>();
		while (!deletelist.isEmpty())
			this.functions.addLast(new LinearFunction(deletelist.poll(), deletelist.poll()));
	}
	
	public void print_functions_list(){
		for (LinearFunction f : this.functions)
			System.out.println("f(x) = " + f.a + "x + " + f.b);
	}
	
	public void print_points_list(){
		for (Point pp : this.points)
			System.out.println("Point " + pp.x + "/" + pp.y);
	}
	
	public void make_functions_from_a_single_point_and_multiple_gradients(){
		this.functions = new LinkedList<LinearFunction>();
		this.points = new LinkedList<Point>();
		this.points.add(this.p);
		Point current_point = p;
		for (int i=0; i<breadth.length; i++){
			double next_x = current_point.x + breadth[i];
			double next_y = current_point.y + breadth[i] * grad[i];
			Point next_point = new Point(next_x, next_y);
			this.points.add(next_point);
			System.out.println("next point: " + next_point.x + " / " + next_point.y);
			LinearFunction next_function = new LinearFunction(current_point, next_point);
			this.functions.add(next_function);
			current_point = next_point;
		}
	}
	
	public double get_y_from_x(double x){
		return this.a * x + b;
	}
	
	public double get_x_from_y(double y){
		return (y - this.b) / this.a;
	}
	
	public double get_max_from_all_functions_for_x(double x){
		double ret_val = -100000.0;
		for (LinearFunction f : this.functions)
			if (f.get_y_from_x(x) > ret_val)
				ret_val = f.get_y_from_x(x);
		return ret_val;
	}
	
	public static void main(String[] args) {
		LinearFunction bla = new LinearFunction();
		bla.print_functions_list();
		bla.print_points_list();
		for (int i = 0; i < 21; i++)
			System.out.println("at " + (i*50) + ":\t" + bla.get_max_from_all_functions_for_x(50.0/1000.0*i));
//		System.out.println("a=" + bla.a  + ", b=" + bla.b);
	}
	
	static LinearFunction get_cost_function_for_load_balancing(){
		double y_val = 1.0;
		LinkedList<Point> thepoints = new LinkedList<Point>();
		thepoints.add(new Point(0.5, 0.0));
		for (int delta_x=1; delta_x<10; delta_x++){
			y_val += Math.pow(2, delta_x-1);
			thepoints.add(new Point(0.5 + 0.05 * delta_x, -1.0 + y_val));
		}
		LinearFunction cost = new LinearFunction(thepoints);
		return cost;
	}
	
	static LinearFunction get_cost_function_for_load_balancing_after_link_failure(){
		double y_val = 1.0;
		LinkedList<Point> thepoints = new LinkedList<Point>();
		thepoints.add(new Point(0.8, 0.0));
		for (int delta_x=1; delta_x<10; delta_x++){
			y_val += Math.pow(2, delta_x-1);
			thepoints.add(new Point(0.8 + 0.05 * delta_x, -1.0 + y_val));
		}
		LinearFunction cost = new LinearFunction(thepoints);
		return cost;
	}
	


}
