import java.util.LinkedList;

public class Multi_IP_Path implements MultiPath{
	private LinkedList<IP_Path> paths;
	private int k = 7; // max number of paths for this source destination pair
	private Router_OSPF source;
	private Router_OSPF destination;
	
	@Override
	public int size(){
		return this.paths.size();
	}
	
	@Override
	public int getK() {
		return k;
	}

	@Override
	public void setK(int k) {
		this.k = k;
		while (this.paths.size() > this.k)
			this.paths.removeLast();
	}

	@Override
	public Router_OSPF getSource() {
		return source;
	}

	@Override
	public Router_OSPF getDestination() {
		return destination;
	}

	public Multi_IP_Path(IP_Path initialPath){
		this.paths = new LinkedList<IP_Path>();
		this.paths.add(initialPath);
		this.source = (Router_OSPF) initialPath.getSource();
		this.destination = (Router_OSPF) initialPath.getDestination();
	}
	
	// constructor to initialize with single hop paths
	public Multi_IP_Path(Router_OSPF source_of_one_hop_path, Router_OSPF destination_of_one_hop_path){
		this(new IP_Path(source_of_one_hop_path, destination_of_one_hop_path));
	}
	
	@Override
	public LinkedList<Path> getPaths(){
		LinkedList<Path> copy = new LinkedList<Path>(this.paths);
		return copy;
	}
	
	@Override
	public boolean add(Path newPath){
		if (!(newPath instanceof IP_Path))
			throw new Error("ERROR: " + newPath.toString() + " is not an instance of IP_Path!");
		if (this.paths.contains(newPath))
			return false;
		if (!newPath.getSource().equals(this.source))
			return false;
		if (!newPath.getDestination().equals(this.destination))
			return false;
		if (this.isFull() && newPath.length() >= this.length_of_longest_path())
			return false;
		int index = 0;
		for (IP_Path p : this.paths)
			if (p.length() < newPath.length())
				index++;
		this.paths.add(index, (IP_Path) newPath);
		if (this.paths.size() > this.k)
			this.paths.removeLast();
		return true;
	}
	
	@Override
	public String toString(){
		String out = "All Paths from " + this.source.getName() + " to " + this.destination.getName() + ":\r\n";
		for (IP_Path p : this.paths)
			out += p.toString() + "\r\n";
		return out;
	}
	
	@Override
	public boolean isFull(){
		return this.paths.size() == this.k;
	}
	
	@Override
	public int length_of_longest_path(){
		return this.paths.getLast().length();
	}

	@Override
	public Path getPath(int index) {
		return this.paths.get(index);
	}
	
}
