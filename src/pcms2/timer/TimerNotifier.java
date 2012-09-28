package pcms2.timer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.MulticastChannel;
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
	private DatagramChannel udpChannel = null;
	private MembershipKey multicastKey = null;
	private SocketAddress multicastAddress = null;
	private String broadcastAddr = null;
	private SocketAddress broadcastAddress = null;

	public void activate() throws ConfigurationException, IOException {
		registerService(null, ITimerNotifier.class, this);
		log.info("Timer notifier service registered");
		try {
			cs = (ClockService) lookupService(ClockService.class);
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		clockId = config.getString("clock-id");

		//FIXME: improve error handling
		//TODO: join channels
		if (config.hasAttribute("multicast-group")) {
			log.info("Initiating multicast notifier");
			if (config.hasAttribute("multicast-port")
					&& config.hasAttribute("multicast-iface")) {
				int multicastPort = config.getInt("multicast-port");
				String group = config.getString("multicast-group");

				InetAddress groupAddr = InetAddress.getByName(group);
				NetworkInterface iface = NetworkInterface.getByName("iface");
				
				multicastAddress = new InetSocketAddress(groupAddr, multicastPort);
				
				MulticastChannel multicastChannel = DatagramChannel.open(StandardProtocolFamily.INET)
						.setOption(StandardSocketOptions.SO_REUSEADDR, true)
						.bind(new InetSocketAddress(multicastPort))
						.setOption(StandardSocketOptions.IP_MULTICAST_IF, iface);
				
				multicastKey = multicastChannel.join(groupAddr, iface);			
				log.info("Multicast notifier initiated");
			} else {
				if (!config.hasAttribute("multicast-port")) {
					log.error("Required param multicast-port is not set. Multicast notifier disabled");
				}

				if (!config.hasAttribute("multicast-iface")) {
					log.error("Required param multicast-iface is not set. Multicast notifier disabled");
				}
			}

		}
		
		if (config.hasAttribute("udp-port")) {
			log.info("Initiating UDP notifier");
			int port = config.getInt("udp-port");
			log.info("UDP notifier initiated");
			
			udpChannel = DatagramChannel.open(StandardProtocolFamily.INET)
					.setOption(StandardSocketOptions.SO_REUSEADDR, true)
					.bind(new InetSocketAddress(port));
		

			if (config.hasAttribute("broadcast-port") && config.hasAttribute("broadcast-addr")) {
				log.warning("Broadcast is very bad idea");
				log.info("Initiating broadcast notifier");
				int broadcastPort = config.getInt("broadcast-port");
				broadcastAddr = config.getString("broadcast-addr");
				
				broadcastAddress = new InetSocketAddress(InetAddress.getByName(broadcastAddr), broadcastPort);
	
				udpChannel.setOption(StandardSocketOptions.SO_BROADCAST, true);
	
				log.info("Broadcast notifier initiated");
			} else if (config.hasAttribute("broadcast-port") || config.hasAttribute("broadcast-addr")) {
				if (!config.hasAttribute("broadcast-port")) {
					log.error("Required param broadcast-port is not set. Multicast notifier disabled");
				}

				if (!config.hasAttribute("broadcast-iface")) {
					log.error("Required param broadcast-iface is not set. Multicast notifier disabled");
				}
			}
		}

		if (config.hasAttribute("tcp-port")) {
			//log.info("Initiating TCP notifier");
			//int port = config.getInt("tcp-port");
			log.error("TCP notifier not implemented");
			//log.info("TCP notifier initiated");
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
					// log.info("I'm alive, " + clockId + "=" + cl.getTime());
					
					ByteBuffer buf = ByteBuffer.allocate(18);
					buf.put((byte) 0x01);
					buf.put((byte) cl.getStatus());
					buf.putLong(cl.getTime());
					buf.putLong(cl.getLength());
					
					if (multicastKey != null) {
						DatagramChannel channel = (DatagramChannel) multicastKey.channel();
						try {
							channel.send(buf, multicastAddress);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
					if (broadcastAddr != null) {
						try {
							udpChannel.send(buf, broadcastAddress);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
					
					
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
