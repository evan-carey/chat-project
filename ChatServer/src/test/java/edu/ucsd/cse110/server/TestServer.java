package edu.ucsd.cse110.server;

import static org.junit.Assert.*;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.MessageCreator;




//@RunWith(SpringJUnit4ClassRunner.class)
public class TestServer {
	
	@Autowired
	private Server2 server = new Server2();
	
	@Before
	public void init() {
		//Server2.main(null);
		server = new Server2();
	}
	
	@Test
	public void testServerNotNull() {
		assertNotNull(server);
		
	}
	
	@Test
	public void TestMessage() throws JMSException {
		TextMessage msg = EasyMock.createMock(TextMessage.class);
		msg.setText("Texty1");
		msg.setJMSCorrelationID("ID1");
		assertEquals(null, msg.getText());
	}
	
	
	@Test
	public void testloggedOn() throws JMSException {
		TextMessage msg = EasyMock.createMock(TextMessage.class);
		//ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
		//Connection connection = connectionFactory.createConnection();
		//connection.start();
		//Session session = session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		//TextMessage msg = session.createTextMessage();
//		Destination D = session.createTopic("Default");
		msg.setText("Texty1");
		msg.setJMSCorrelationID("ID1");
//		msg.setJMSReplyTo(D);
		//server.onMessage(msg);
		assertTrue(server.getUserMap().isEmpty());
	}
	


}
