package me.lpk.hijack;

import java.io.File;
import java.lang.management.ManagementFactory;

import com.sun.tools.attach.VirtualMachine;

public class Loader {
	public static void loadAgent(String jarFilePath) {
		String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
		int p = nameOfRunningVM.indexOf('@');
		String pid = nameOfRunningVM.substring(0, p);
		try {
			File f = new File(jarFilePath);
			System.out.println("Loading agent: " + f.getPath());
			VirtualMachine vm = VirtualMachine.attach(pid);
			vm.loadAgent(f.getAbsolutePath(), "");
			
			VirtualMachine.attach(vm.id());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}