package pcms2.timer;
import java.rmi.RemoteException;

import pcms2.framework.BaseComponent;
import pcms2.framework.IThread;
import pcms2.framework.config.ConfigurationException;
import pcms2.services.site.Clock;
import pcms2.services.site.ClockService;
import pcms2.services.site.NoSuchClockException;

public class TimerNotifier extends BaseComponent implements ITimerNotifier {
	private IThread worker;
	private ClockService cs;
	private String clockId;
	
		
	public void activate() throws ConfigurationException {
		registerService(null, ITimerNotifier.class, this);
		log.info("Timer notifier service registered");
		try {
			cs = (ClockService) lookupService(ClockService.class);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		clockId = config.getString("clock-id");
				
		if (config.hasAttribute("multicast-group")) {
			log.info("Initiating multicast notifier");
			if (config.hasAttribute("multicast-port")) {
				int port = config.getInt("multicast-port");
				String group = config.getString("multicast-group");
				
				log.info("Multicast notifier initiated");
			} else {
				log.error("Required param multicast-port is not set. Multicast notifier disabled");
			}
			
		}
		
		if (config.hasAttribute("broadcast-addr")) {
			log.warning("Broadcast is very bad idea");
			log.info("Initiating broadcast notifier");
			if (config.hasAttribute("broadcast-port")) {
				int port = config.getInt("broadcast-port");
				String addr = config.getString("broadcast-addr");
				
				log.info("Broadcast notifier initiated");
			} else {
				log.error("Required param broadcast-port is not set. Broadcast notifier disabled");
			}
		}
		
		if (config.hasAttribute("udp-port")) {
			log.info("Initiating UDP notifier");
			int port = config.getInt("udp-port");
			log.info("UDP notifier initiated");
		}
		
		if (config.hasAttribute("tcp-port")) {
			log.info("Initiating TCP notifier");
			int port = config.getInt("tcp-port");
			log.info("TCP notifier initiated");
		}
		
		
		worker = startThread(this, "timer-notifier-thread");
	}
	
	public void passivate() {
		worker.interrupt();
		try {
			worker.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				try {
					Clock cl = cs.getClock(clockId);
					//log.info("I'm alive, " + clockId + "=" + cl.getTime());
					int a = cl.getStatus();
				} catch (RemoteException | NoSuchClockException e) {
					e.printStackTrace();
				}
				synchronized (this) {
					this.wait(400);
				}
			} catch (InterruptedException e) {
				return;
			}
		}
	}

}
