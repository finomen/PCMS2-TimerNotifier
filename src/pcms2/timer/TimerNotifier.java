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
	public void activate() {
		registerService(null, ITimerNotifier.class, this);
		log.info("Timer notifier service registered");
		try {
			cs = (ClockService) lookupService(ClockService.class);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		try {
			clockId = config.getString("clock-id");
		} catch (ConfigurationException e) {
			e.printStackTrace();
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
