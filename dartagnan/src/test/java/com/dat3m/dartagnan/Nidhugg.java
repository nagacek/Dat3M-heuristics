package com.dat3m.dartagnan;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static com.dat3m.dartagnan.utils.ResourceHelper.getCSVFileName;
import static com.dat3m.dartagnan.utils.ResourceHelper.initialiseCSVFile;

@RunWith(Parameterized.class)
public class Nidhugg {

	// These tests are supposed to be run in conjunction with com.dat3m.dartagnan.CLocksTest
	// We use com.dat3m.dartagnan.CLocksTest.TIMEOUT to guarantee fairness in the comparison 

    private final String path;

	@Parameterized.Parameters(name = "{index}: {0} target={2}")
    public static Iterable<Object[]> data() throws IOException {

    	// We want the files to be created every time we run the unit tests
        initialiseCSVFile(Nidhugg.class, "nidhugg");

		List<Object[]> data = new ArrayList<>();

        data.add(new Object[]{System.getenv("DAT3M_HOME") + "/benchmarks/locks/ttas-5.c"});
        data.add(new Object[]{System.getenv("DAT3M_HOME") + "/benchmarks/locks/ttas-5-acq2rx.c"});
        data.add(new Object[]{System.getenv("DAT3M_HOME") + "/benchmarks/locks/ttas-5-rel2rx.c"});

        data.add(new Object[]{System.getenv("DAT3M_HOME") + "/benchmarks/locks/ticketlock-3.c"});
        data.add(new Object[]{System.getenv("DAT3M_HOME") + "/benchmarks/locks/ticketlock-3-acq2rx.c"});
        data.add(new Object[]{System.getenv("DAT3M_HOME") + "/benchmarks/locks/ticketlock-3-rel2rx.c"});

        data.add(new Object[]{System.getenv("DAT3M_HOME") + "/benchmarks/locks/mutex-3.c"});
        data.add(new Object[]{System.getenv("DAT3M_HOME") + "/benchmarks/locks/mutex-3-acq2rx-futex.c"});
        data.add(new Object[]{System.getenv("DAT3M_HOME") + "/benchmarks/locks/mutex-3-acq2rx-lock.c"});
        data.add(new Object[]{System.getenv("DAT3M_HOME") + "/benchmarks/locks/mutex-3-rel2rx-futex.c"});
        data.add(new Object[]{System.getenv("DAT3M_HOME") + "/benchmarks/locks/mutex-3-rel2rx-unlock.c"});

        data.add(new Object[]{System.getenv("DAT3M_HOME") + "/benchmarks/locks/spinlock-5.c"});
        data.add(new Object[]{System.getenv("DAT3M_HOME") + "/benchmarks/locks/spinlock-5-acq2rx.c"});
        data.add(new Object[]{System.getenv("DAT3M_HOME") + "/benchmarks/locks/spinlock-5-rel2rx.c"});
        
        data.add(new Object[]{System.getenv("DAT3M_HOME") + "/benchmarks/locks/linuxrwlock-3.c"});
        data.add(new Object[]{System.getenv("DAT3M_HOME") + "/benchmarks/locks/linuxrwlock-3-acq2rx.c"});
        data.add(new Object[]{System.getenv("DAT3M_HOME") + "/benchmarks/locks/linuxrwlock-3-rel2rx.c"});
        
        data.add(new Object[]{System.getenv("DAT3M_HOME") + "/benchmarks/locks/mutex_musl-3.c"});
        data.add(new Object[]{System.getenv("DAT3M_HOME") + "/benchmarks/locks/mutex_musl-3-acq2rx-futex.c"});
        data.add(new Object[]{System.getenv("DAT3M_HOME") + "/benchmarks/locks/mutex_musl-3-acq2rx-lock.c"});
        data.add(new Object[]{System.getenv("DAT3M_HOME") + "/benchmarks/locks/mutex_musl-3-rel2rx-futex.c"});
        data.add(new Object[]{System.getenv("DAT3M_HOME") + "/benchmarks/locks/mutex_musl-3-rel2rx-unlock.c"});

        return data;
    }

    public Nidhugg(String path) {
        this.path = path;
    }

    @Test
    public void test() {
    	
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(getCSVFileName(getClass(), "nidhugg"), true)))
           {
        		writer.newLine();
        		writer.append(path.substring(path.lastIndexOf("/") + 1)).append(", ");
        		// The flush() is required to write the content in the presence of timeouts
        		writer.flush();
        		
        		int bound = 1;
        		toLLVM(path);
        		unroll(path, bound);
        		
            	String output = "";
            	String wOutput = "";
            	ArrayList<String> cmd = new ArrayList<String>();
            	cmd.add("nidhugg");
            	cmd.add("--tso");
            	cmd.add(String.format("%s.u%s.ll", path, bound));
            	ProcessBuilder processBuilder = new ProcessBuilder(cmd);

            	long start = System.currentTimeMillis();
	           	Process proc = processBuilder.start();
	           	// Warnings are not captured by the input stream and we need them to decide if using !\ytick
	           	BufferedReader readW = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
	   			BufferedReader read = new BufferedReader(new InputStreamReader(proc.getInputStream()));
	   			proc.waitFor();
	   			while(readW.ready()) {
	   				wOutput = wOutput + readW.readLine();
	   			}
	   			while(read.ready()) {
	   				output = output + read.readLine();
	   			}
	   			if(proc.exitValue() == 1) {
	   				BufferedReader error = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
	   				while(error.ready()) {
	   					System.out.println(error.readLine());
	   				}
	   			}
	   			long solvingTime = System.currentTimeMillis() - start;
                String result = output.contains("No errors were detected") ? 
                							wOutput.contains("WARNING") ? "!\\ytick" : "\\gtick" : 
                							"\\redcross";

                writer.append(result).append(", ").append(Long.toString(solvingTime));
           } catch (Exception e){
        	   System.out.println(String.format("%s failed with the following msg: %s", path, e.getMessage()));
           }
    }
    
    private void toLLVM(String path) {
    	try {
        	ArrayList<String> cmd = new ArrayList<String>();
        	cmd.add("clang");
        	cmd.add("-emit-llvm");
        	cmd.add("-S");
        	cmd.add("-o");
        	cmd.add(path + ".ll");
        	cmd.add(path);
        	ProcessBuilder processBuilder = new ProcessBuilder(cmd);

           	Process proc = processBuilder.start();
    		proc.waitFor();
    		if(proc.exitValue() == 1) {
    			BufferedReader error = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
    			while(error.ready()) {
    				System.out.println(error.readLine());
    			}
    		}
    	} catch(Exception e) {
    		System.out.println(String.format("%s failed with the following msg: %s", path, e.getMessage()));
    	}
    }

    private void unroll(String path, int bound) {
    	try {
        	ArrayList<String> cmd = new ArrayList<String>();
        	cmd.add("nidhugg");
        	cmd.add("--unroll=" + bound);
        	cmd.add(String.format("--transform=%s.u%s.ll", path, bound));
        	cmd.add(path + ".ll");
        	ProcessBuilder processBuilder = new ProcessBuilder(cmd);

           	Process proc = processBuilder.start();
    		proc.waitFor();
    		if(proc.exitValue() == 1) {
    			BufferedReader error = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
    			while(error.ready()) {
    				System.out.println(error.readLine());
    			}
    		}
    	} catch(Exception e) {
    		System.out.println(String.format("%s failed with the following msg: %s", path, e.getMessage()));
    	}
    }
}