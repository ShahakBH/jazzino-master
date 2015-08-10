import com.yazino.configuration.YazinoConfiguration;
import com.yazino.email.EmailValidationResolver;
import com.yazino.email.simple.SimpleEmailValidator;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class StandaloneEmailValidation {

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Reading source file");
        BufferedReader reader = new BufferedReader(new InputStreamReader(StandaloneEmailValidation.class.getResourceAsStream("emails.txt")));
        List<String> emails = new ArrayList<String>();
        String line;
        while ((line = reader.readLine()) != null) {
            emails.add(line.split(",")[1].trim());
        }
        reader.close();
        System.out.println("Checking " + emails.size() + " emails.");
        final ValidationService service = new ValidationService();
        Map<String, Boolean> result = service.checkEmails(emails);
        int valid = 0;
        final BufferedWriter writer = new BufferedWriter(new FileWriter("/tmp/valid_yazino_emails.txt"));
        for (String email : result.keySet()) {
            if (result.get(email)) {
                writer.write(email);
                writer.newLine();
                writer.flush();
                valid++;
            }
        }
        writer.close();
        System.out.println(valid + "/" + emails.size() + " are valid.");
        service.shutdown();
    }

    public static class ValidationService extends ThreadPoolExecutor {

        private final YazinoConfiguration yazinoConfiguration = new YazinoConfiguration();
        private final SimpleEmailValidator validator = new SimpleEmailValidator();
        private final EmailValidationResolver validationResolver = new EmailValidationResolver(yazinoConfiguration);

        public ValidationService() {
            super(1000, 1000, 1, TimeUnit.MINUTES, new LinkedBlockingDeque<Runnable>());
        }

        public Map<String, Boolean> checkEmails(final List<String> emails) throws InterruptedException, IOException {
            final CountDownLatch countdown = new CountDownLatch(emails.size());

            final Map<String, Boolean> result = new ConcurrentHashMap<String, Boolean>();
            for (final String email : emails) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            boolean validate = validationResolver.isValid(validator.validate(email));
                            result.put(email, validate);
                            long count = countdown.getCount();
                            System.err.println(emails.size() - count + " done.");
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            countdown.countDown();
                        }
                    }
                });
            }
            countdown.await();
            return result;
        }
    }
}

