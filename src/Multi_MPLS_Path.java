import java.util.LinkedList;

public class Multi_MPLS_Path implements MultiPath{
	
	private LinkedList<MPLS_Path> paths;
	private int k = 12; // max number of paths for this source destination pair
	private Router_MPLS source;
	private Router_MPLS destination;
	
	public Multi_MPLS_Path(MPLS_Path initialPath){
		this.paths = new LinkedList<MPLS_Path>();
		this.paths.add(initialPath);
		this.source = (Router_MPLS) initialPath.getSource();
		this.destination = (Router_MPLS) initialPath.getDestination();
	}
	
	@Override
	public int size() {
		return this.paths.size();
	}

	@Override
	public int getK() {
		return this.k;
	}

	@Override
	public void setK(int k) {
		this.k = k;
		while (this.paths.size() > this.k)
			this.paths.removeLast();
	}

	@Override
	public Router getSource() {
		return this.source;
	}

	@Override
	public Router getDestination() {
		return this.destination;
	}

	@Override
	public Path getPath(int index) {
		return this.paths.get(index);
	}

	@Override
	public LinkedList<Path> getPaths() {
		return new LinkedList<Path>(this.paths);
	}

	@Override
	public boolean add(Path newPath) {
		if (!(newPath instanceof MPLS_Path))
			throw new Error("ERROR: " + newPath.toString() + " is not an instance of MPLS_Path!");
		if (this.paths.contains(newPath))
			return false;
		if (!newPath.getSource().equals(this.source))
			return false;
		if (!newPath.getDestination().equals(this.destination))
			return false;
		if (this.isFull() && newPath.length() >= this.length_of_longest_path())
			return false;
		int index = 0;
		for (MPLS_Path p : this.paths)
			if (p.length() < newPath.length())
				index++;
		this.paths.add(index, (MPLS_Path) newPath);
		if (this.paths.size() > this.k)
			this.paths.removeLast();
		return true;
	}

	@Override
	public boolean isFull() {
		return this.paths.size() == this.k;
	}

	@Override
	public int length_of_longest_path() {
		return this.paths.getLast().length();
	}
	
	@Override
	public String toString(){
		String out = this.paths.size() + " shortest paths from " + this.source.getName() + " to " + this.getDestination().getName() + ":\r\n";
		for (MPLS_Path path : this.paths)
			out += path.toString() + "\r\n";
		return out;
	}
}
