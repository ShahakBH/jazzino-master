package senet.server.table;

import java.util.List;

import com.yazino.platform.model.table.Client;
import com.yazino.platform.repository.table.ClientRepository;

public class InMemoryClientRepository implements ClientRepository {
	
	
	public Client[] findAll(String gameType) {
		return defaultClients.toArray(new Client[0]);
	}
	public Client findById(String clientId) {
		for (Client c:defaultClients){
			if (c.getClientId().equals(clientId)) return c;
		}
		return null;
	}
	private List<Client> defaultClients;
	public void setDefaultClients(List<Client> defaultClients) {
		this.defaultClients = defaultClients;
	}

}
