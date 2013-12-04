package edu.ucsd.cse110.server;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;


@Configuration
@ComponentScan
public class SpringServer {
   //private JmsTemplate jmsTemplate;
   @Bean
   ConnectionFactory connectionFactory() {
       return new CachingConnectionFactory(
               new ActiveMQConnectionFactory(ServerConstants.messageBrokerUrl));
   }
   
   @Bean
   MessageListenerAdapter receiver() {
       return new MessageListenerAdapter(new Server()) {{
           setDefaultListenerMethod("receive");
       }};
   }
   
   @Bean
   SimpleMessageListenerContainer container(final MessageListenerAdapter messageListener,
           final ConnectionFactory connectionFactory) {
       return new SimpleMessageListenerContainer() {{
           setMessageListener(messageListener);
           setConnectionFactory(connectionFactory);
           setDestinationName(ServerConstants.messageTopicName);
       }};
   }

   @Bean
   JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
       return new JmsTemplate(connectionFactory);
   }
   

	public static void main(String[] args) throws Throwable {
		BrokerService broker = new BrokerService();
		broker.addConnector(ServerConstants.messageBrokerUrl);
		broker.setPersistent(false);
		broker.start();
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SpringServer.class);
		
		MessageCreator messageCreator = new MessageCreator() {
			public Message createMessage(Session session) throws JMSException {
				return session.createTextMessage("ping!");
			}
       }; 
       
       JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
       System.out.println("Sending a new message:");
       jmsTemplate.send(ServerConstants.messageTopicName, messageCreator);

       //context.close();
	}
}
