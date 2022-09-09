package intraloadbalancingft;

        import javax.jms.Connection;
        import javax.jms.ConnectionFactory;
        import javax.jms.Destination;
        import javax.jms.JMSException;
        import javax.jms.MessageProducer;
        import javax.jms.Session;
        import javax.jms.TextMessage;

        import org.apache.activemq.ActiveMQConnection;
        import org.apache.activemq.ActiveMQConnectionFactory;

public class DecisionRequest {

    private static String url = ActiveMQConnection.DEFAULT_BROKER_URL;
    private static String subject = "DecisionRequests"; // Queue Name. You can create any/many queue names as per your requirement.

    public DecisionRequest() {

    }


    // default broker URL is : tcp://localhost:61616"

    public void produceMessages(String message) {
        MessageProducer messageProducer;
        TextMessage textMessage;
        try {

            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
            Connection connection = connectionFactory.createConnection();
            connection.start();

            Session session = connection.createSession(false /*Transacter*/, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(subject);

            messageProducer = session.createProducer(destination);
            textMessage = session.createTextMessage();

            textMessage.setText(message);
            //System.out.println("Sending the following message: " + textMessage.getText());
            messageProducer.send(textMessage);

            messageProducer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

}



// Python code for sending and receiving messages from ActiveMQ queues
//import stomp
//        import time
//
//        conn = stomp.Connection()
//        conn.connect("admin", "admin", wait=True)
//        conn.send(body="hi", destination='DecisionReplies')
//        conn.disconnect()
//
//
//class MyListener(stomp.ConnectionListener):
//        def on_error(self, headers, message):
//        print('received an error "%s"' % message)
//
//        def on_message(self, message):
//        print("received a message %s" % message)
//
//        conn = stomp.Connection()
//        conn.set_listener("", MyListener())
//        conn.connect("admin", "admin", wait=True)
//
//        conn.subscribe(destination="DecisionRequests", id=1, ack="auto")
//
//        time.sleep(2)
//        conn.disconnect()


// OUTPUT
//received a message {cmd=MESSAGE,headers=[{'expires': '0', 'destination': '/queue/DecisionRequests', 'subscription': '1', 'priority': '4', 'message-id': 'ID:MSI-63981-1651810433599-9:1:1:1:1', 'persistent': 'true', 'timestamp': '1651810447879'}],body=HostAgent21}
//received a message {cmd=MESSAGE,headers=[{'expires': '0', 'destination': '/queue/DecisionRequests', 'subscription': '1', 'priority': '4', 'message-id': 'ID:MSI-63981-1651810433599-11:1:1:1:1', 'persistent': 'true', 'timestamp': '1651810449514'}],body=HostAgent31}