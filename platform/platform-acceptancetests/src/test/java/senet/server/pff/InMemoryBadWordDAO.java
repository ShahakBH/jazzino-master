package senet.server.pff;

import com.yazino.platform.persistence.community.BadWordDAO;

import java.util.HashSet;
import java.util.Set;

public class InMemoryBadWordDAO implements BadWordDAO {
	private final Set<String> badWords = new HashSet<String>();
	public final Set<String> partBadWords = new HashSet<String>();

	public Set<String> findAllBadWords() {
		return this.badWords;
	}
	
	public Set<String> findAllPartBadWords() {
		return this.partBadWords;
	}

	public void addBarWord(String badWord) {
		badWords.add(badWord);
	}
	
	public void addPartBadWord(String badWord) {
		partBadWords.add(badWord);
	}
}
