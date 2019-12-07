package copy.base.fetcher.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

public class ClientLowerCaseProcessor implements ItemProcessor<Client, Client> {

    private static final Logger log = LoggerFactory.getLogger(ClientLowerCaseProcessor.class);

    @Override
    public Client process(Client client) {
        Long id = client.getId();
        String firstName = client.getFirstName().toLowerCase();
        String lastName = client.getLastName().toLowerCase();
        String email = client.getEmail().toLowerCase();
        String phone = client.getPhone();

        log.info("Converting ("+client+")");

        return new Client(id, firstName, lastName, email, phone);
    }
}
