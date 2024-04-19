package client.utils;

import commons.Event;
import commons.Expense;
import commons.User;
import commons.UserExpense;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class EmailUtils {

    private static Session prepareMail(String fromEmail, String password) {
        Properties props = new Properties();

        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.starttls.enable", true);
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.user", fromEmail);
        props.put("mail.smtp.password", password);
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", true);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, password);
            }
        });

        return session;

    }

    /**
     * sends a default Email
     * @throws Exception
     */
    public static void sendDefault(String fromEmail, String password) throws Exception {

        Session session = prepareMail(fromEmail, password);

        Message message = new MimeMessage(session);
        Address from = new InternetAddress(fromEmail);

        message.setFrom(from);
        message.addRecipient(Message.RecipientType.CC, from);

        message.setSubject("Test Email");
        message.setText(
                "This is a test email to check that your Email and Password\n" +
                        "Configuration is correct for the Splitty Program"
        );

        Transport.send(message);

    }


    /**
     * Creates and sends the emails
     * @param emails to send to
     * @param event info for text
     */
    public static void sendInvites(String fromEmail,
                            String password,
                            String emails,
                            Event event) throws Exception {

        //converts line into email and username lists
        Scanner input = new Scanner(emails);
        LinkedList<String> emailList = new LinkedList<>();
        LinkedList<String> usernameList = new LinkedList<>();
        while (input.hasNextLine()) {
            Scanner line = new Scanner(input.next());
            line.useDelimiter(":");
            emailList.add(line.next().trim());
            usernameList.add(line.next().trim().toLowerCase());
        }

        //convert emails to string
        emails = "";
        int sizeE = emailList.size();
        for (int i = 0; i < sizeE; i++) {
            emails = emails + emailList.get(i);
            if (i < sizeE - 1) {
                emails = emails + ",";
            }
        }

        //create email template
        Session session = prepareMail(fromEmail, password);
        Message message = new MimeMessage(session);
        Address[] recipients = InternetAddress.parse(emails);
        Address from = new InternetAddress(fromEmail);

        //set recipients
        message.setFrom(from);
        message.addRecipients(Message.RecipientType.BCC, recipients);
        message.addRecipient(Message.RecipientType.CC, from);

        //set text
        message.setSubject("Invitation To Splitty Event: " + event.getTitle());
        message.setText(
                "You Have been invited to join a Splitty for " + event.getTitle() +
                        "\nTo join, go on Splitty and enter this Invite Code :" +
                        "\n" + event.getInviteCode() +
                        "\nAnd this Server URL :" +
                        "\n" + ConfigUtils.getServerUrl()
        );
        Transport.send(message);

        //add users once email is sent
        int sizeU = usernameList.size();
        for (int i = 0; i < sizeU; i++) {
            User user = UserUtils.createOrGet(usernameList.get(i));
            ConfigUtils.setEmail(user.getId(), emailList.get(i));
            EventUtils.joinEvent(ConfigUtils.getCurrentEvent(), user);
        }
    }

    /**
     * Creates and sends the email
     * @param toEmail to send to
     * @param event info for text
     * @param uExpense expense user is part of
     */
    public static void sendReminderOne(String fromEmail,
                                String password,
                                String toEmail,
                                Event event,
                                UserExpense uExpense) throws Exception {

        Session session = prepareMail(fromEmail, password);
        Message message = new MimeMessage(session);
        Address from = new InternetAddress(fromEmail);
        Address to = new InternetAddress(toEmail);

        message.setFrom(from);
        message.addRecipient(Message.RecipientType.BCC, from); //back to yourself
        message.addRecipient(Message.RecipientType.CC, to);

        message.setSubject("Payment Reminder for Splitty Event: " + event.getTitle());
        message.setText(
                "This is a reminder for payment for: " + uExpense.getExpense().getTitle() +
                        "\nWhich was originally paid by: " + uExpense.getExpense().getOriginalPayer().getUsername() +
                        "\n-----" +
                        "\nYou need to pay: " + uExpense.getTotalAmount() +
                        "\nYou have so far paid: " + uExpense.getPaidAmount()
        );
        Transport.send(message);
    }

    /**
     * Creates and sends the emails
     * @param rowItems emails to send to
     * @param event info for text
     * @param expense expense users are part of
     */
    public static void sendReminderMany(String fromEmail,
                                 String password,
                                 List<UserExpense> rowItems,
                                 Event event,
                                 Expense expense) throws Exception {

        LinkedList<String> list = new LinkedList<>();
        for (UserExpense ue : rowItems) {
            String e = ConfigUtils.getEmail(ue.getDebtor().getId()).trim().toLowerCase();
            if (e != null) {
                list.add(e);
            }
        }
        String emails = "";
        while (!list.isEmpty()) {
            emails = emails + list.removeFirst();
            if (!list.isEmpty()) {
                emails = emails + ",";
            }
        }

        Session session = prepareMail(fromEmail, password);
        Message message = new MimeMessage(session);
        Address[] recipients = InternetAddress.parse(emails);
        Address from = new InternetAddress(fromEmail);

        message.setFrom(from);
        message.addRecipients(Message.RecipientType.BCC, recipients);
        message.addRecipient(Message.RecipientType.CC, from);

        message.setSubject("Payment Reminder for Splitty Event: " + event.getTitle());
        message.setText(
                "This is a reminder for payment for: " + expense.getTitle() +
                        "\nWhich was originally paid by: " + expense.getOriginalPayer().getUsername()
        );
        Transport.send(message);
    }
}
