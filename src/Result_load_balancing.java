import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

public class Result_load_balancing implements Serializable{
	
	private class linkResult implements Serializable{
		private static final long serialVersionUID = -4322696679161389510L;
		public double load;
		public Integer bandwidth;
		
		public linkResult(double load, int bandwidth){
			this.load = load;
			this.bandwidth = bandwidth;
		}
		
		public double excessLoad(){
			if (this.load < this.bandwidth)
				return 0.0;
			return this.load - this.bandwidth;
		}
		
		public double utilization(){
			return this.load / (1.0*this.bandwidth);
		}
	}
	
	private static final long serialVersionUID = 8832191412718948216L;
	private LinkedList<linkResult> results;
	public Integer metricReconfigurations;		// metricChanges times the number of OSPF routers in this sub-domain
	public String sndLibFileName;
	public String networkType;
	public String failedLink;
	
	public Result_load_balancing(){
		this.results = new LinkedList<linkResult>();
		this.metricReconfigurations = 0;
	}
	
	public void writeToDisk(){
		String filename = ".\\results\\" + this.hashCode() + ".dat";
		try {
			FileOutputStream f_out = new FileOutputStream(filename);
			ObjectOutputStream obj_out = new ObjectOutputStream (f_out);
			obj_out.writeObject(this);
			obj_out.close();
		}
		catch (IOException e) {e.printStackTrace();}
	}
	
	private LinkedList<Result_load_balancing> merge(int number_of_link_failures){
		LinkedList<Result_load_balancing> list_of_merged = new LinkedList<Result_load_balancing>();		
		File folder = new File(".\\results");
		File[] files = folder.listFiles();
		HashMap<File, String> sndF = new HashMap<File, String>();
		HashMap<File, String> netwT = new HashMap<File, String>();
		HashMap<File, String> failedL = new HashMap<File, String>();
		LinkedList<File> goodFiles = new LinkedList<File>();
		for (File f : files)
			this.addHeaderInfo(f, sndF, netwT, failedL);
		for (File f : files)
			if ((failedL.get(f).equals("none") && number_of_link_failures == 0)
					|| (!failedL.get(f).equals("none") && number_of_link_failures == 1))
				goodFiles.add(f);
		for (File f : goodFiles){
			Result_load_balancing merged = null;
			for (Result_load_balancing someMerged : list_of_merged)
				if (netwT.get(f).equals(someMerged.networkType) && !failedL.get(f).equals("none"))
					merged = someMerged;
			if (merged != null)
				merged.results.addAll(read(f).results);
			if (merged == null)
				list_of_merged.add(this.read(f));
		}
		return list_of_merged;
	}
	
	private Result_load_balancing read(File f) {
		FileInputStream f_in = null;
		ObjectInputStream obj_in = null;
		Object obj = null;
		try {f_in = new FileInputStream(f);}
		catch (FileNotFoundException e) {e.printStackTrace();}
		try {obj_in = new ObjectInputStream (f_in);}
		catch (IOException e) {e.printStackTrace();}
		try {obj = obj_in.readObject();}
		catch (ClassNotFoundException | IOException e) {e.printStackTrace();}
		if (!(obj instanceof Result_load_balancing))
			return null;
		Result_load_balancing resultObj = (Result_load_balancing) obj;
		return resultObj;
	}

	public void printAllHeaders(){
		File folder = new File(".\\results");
		File[] files = folder.listFiles();
		HashMap<File, String> sndF = new HashMap<File, String>();
		HashMap<File, String> netwT = new HashMap<File, String>();
		HashMap<File, String> failedL = new HashMap<File, String>();
		for (File f : files)
			this.addHeaderInfo(f, sndF, netwT, failedL);
		LinkedList<File> sorted = this.sort(sndF, netwT, failedL);
		for (File f : sorted)
			System.out.println(sndF.get(f) + "    " + failedL.get(f) + "    " + netwT.get(f));
	}
	
	private void addHeaderInfo(File f, HashMap<File, String> sndF, HashMap<File, String> netwT, HashMap<File, String> failedL){
		FileInputStream f_in = null;
		ObjectInputStream obj_in = null;
		Object obj = null;
		try {f_in = new FileInputStream(f);}
		catch (FileNotFoundException e) {e.printStackTrace();}
		try {obj_in = new ObjectInputStream (f_in);}
		catch (IOException e) {e.printStackTrace();}
		try {obj = obj_in.readObject();}
		catch (ClassNotFoundException | IOException e) {e.printStackTrace();}
		if (obj instanceof Result_load_balancing){
			Result_load_balancing result = (Result_load_balancing) obj;
			sndF.put(f, result.sndLibFileName);
			netwT.put(f, result.networkType);
			failedL.put(f, result.failedLink);
		}
	}
	
	private LinkedList<File> sort(HashMap<File, String> sndF, HashMap<File, String> netwT, HashMap<File, String> failedL){
		LinkedList<File> unsortedList = new LinkedList<File>(sndF.keySet());
		LinkedList<File> sortedList = new LinkedList<File>();
		while(!unsortedList.isEmpty()){
			File next = unsortedList.getFirst();
			for (File res : unsortedList){
				if (sndF.get(next).compareTo(sndF.get(res)) > 0){
					next = res;
					continue;
				}
				if (sndF.get(next).compareTo(sndF.get(res)) == 0){
					if (failedL.get(next).compareTo(failedL.get(res)) > 0){
						next = res;
						continue;
					}
					if (failedL.get(next).compareTo(failedL.get(res)) == 0){
						if (netwT.get(next).compareTo(netwT.get(res)) > 0){
							next = res;
							continue;
						}
					}
				}
			}
			sortedList.add(next);
			unsortedList.remove(next);
		}
		return sortedList;
	}

	public void add(Double load, Integer bandwidth){
		this.results.add(new linkResult(load, bandwidth));
	}
	
	public Double getRelativeExcessTrafficDemand(){
		double totalDemand = 0.0;
		double totalExcessDemand = 0.0;
		for (linkResult linkR : this.results){
			totalDemand += linkR.load;
			totalExcessDemand += linkR.excessLoad();
		}
		return totalExcessDemand / totalDemand;
	}
	
	
	public int[] getUtilizationHistogram(double width){
		int number_of_bins = (int) Math.ceil(1.0/width) + 100;
		int[] histogram = new int[number_of_bins];
		for (linkResult linkR : this.results){
			int bin = (int) Math.floor(bound(linkR.utilization(), 1.0) / width);
			histogram[bin]++;
		}
		return histogram;
	}
	
	public void printDensity(double bandwidth, double delta_x){
		System.out.println("\r\nProbability density:");
		System.out.println("====================");
		System.out.print("input file: " + this.sndLibFileName);
		System.out.print(", network type: " + this.networkType);
		System.out.println(", link failure: " + this.failedLink);
		double x = 0.0;
		while (x < 1.05){
			double y = this.kernelDensityEstimation(x, bandwidth);
			if (y < 0.001)
				y = 0.0;
			System.out.println(x + "\t" + y);
			x += delta_x;
		}
	}
	
	private Double gaussKernel(double in){
		double a = Math.exp(-0.5 * Math.pow(in, 2));
		double b = 1.0 / Math.sqrt(2.0 * Math.PI);
		return a*b;
	}
	
	public Double kernelDensityEstimation(double x, double bandwidth){
		double out = 0.0;
		for (linkResult linkR : this.results){
			double in = (x - bound(linkR.utilization(), 1.0)) / bandwidth;
			out += gaussKernel(in);
		}
		return out / (bandwidth * this.results.size());
	}
	
	private double bound(double utilization, double upperBound) {
		if (utilization > upperBound)
			return upperBound;
		return utilization;
	}
	

	@Override
	public boolean equals(Object other){
	    if (other == null) return false;
	    if (other == this) return true;
	    if (!(other instanceof Result_load_balancing)) return false;
	    Result_load_balancing otherResult = (Result_load_balancing) other;
	    if (!otherResult.sndLibFileName.equals(this.sndLibFileName)) return false;
	    if (!otherResult.networkType.equals(this.networkType)) return false;
	    if (!otherResult.failedLink.equals(this.failedLink)) return false;
	    return otherResult.results.size() == this.results.size();
	}
	
    @Override
    public int hashCode() {
    	String identity = this.sndLibFileName + this.networkType + this.failedLink;
        return identity.hashCode();
    }
    
    public static void main(String[] args) {
    	Result_load_balancing test = new Result_load_balancing();
    	LinkedList<Result_load_balancing> merged = test.merge(0);
    	test.printHistogramExcelFormat(merged);
//    	test.printExcelFormat(merged);
    }

	private void printExcelFormat(LinkedList<Result_load_balancing> merged) {
		NumberFormat formatter = new DecimalFormat("#0.0000");     
		System.out.print("utilization");
		for (Result_load_balancing res : merged)
			System.out.print("\t" + res.networkType + "(" + res.results.size() + ")");
		System.out.println();
		double bandwidth = 0.0001;
		double delta_x = 0.05;
		double x = 0.0;
		while (x <= 1.0001){
			System.out.print(formatter.format(x));
			for (Result_load_balancing res : merged){
				double y = res.kernelDensityEstimation(x, bandwidth);
				if (y < 0.001)
					y = 0.0;
				System.out.print("\t" + formatter.format(y));
			}
			System.out.println();
			x += delta_x;
		}
	}
	
	private void printHistogramExcelFormat(LinkedList<Result_load_balancing> merged) {
		NumberFormat formatter = new DecimalFormat("#0.00");     
		System.out.print("utilization");
		for (Result_load_balancing res : merged)
			System.out.print("\t" + res.networkType + "(" + res.results.size() + ")");
		System.out.println();
		HashMap<Result_load_balancing, int[]> histograms = new HashMap<Result_load_balancing, int[]>();
		double delta_x = 0.05;
		for (Result_load_balancing res : merged)
			histograms.put(res, res.getUtilizationHistogram(delta_x));
		int index = 0;
		while (index * delta_x <= 1.0001){
			System.out.print(formatter.format(index * delta_x));
			for (Result_load_balancing res : merged)
				System.out.print("\t" + formatter.format(100.0*histograms.get(res)[index]/122.0));
			System.out.println();
			index++;
		}
	}
	
}
