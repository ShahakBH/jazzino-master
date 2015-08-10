package senet.server.community;

import com.yazino.platform.community.ProfanityService;
import fit.ColumnFixture;
import fit.Fixture;
import fitlibrary.DoFixture;
import fitlibrary.SetUpFixture;

import java.util.HashSet;
import java.util.Set;

public class ProfanityFilter extends DoFixture {

    Set<String> badWords = new HashSet<String>();
    Set<String> partBadWords = new HashSet<String>();

    public Fixture profanityFiltering() {
        return new FilterFixture();
    }

    public class FilterFixture extends ColumnFixture {
        public String comment;
        public String originalText;

        public String filteredText() {
            com.yazino.platform.community.ProfanityFilter filter = new com.yazino.platform.community.ProfanityFilter(new ProfanityService() {
                @Override
                public Set<String> findAllProhibitedWords() {
                    return badWords;
                }

                @Override
                public Set<String> findAllProhibitedPartWords() {
                    return partBadWords;
                }
            });
            return filter.filter(originalText);
        }
    }

    public class WordFixture extends SetUpFixture {

        public void wordFindAsPartOfWord(String s, boolean findAsPartOfWord) {
            if (findAsPartOfWord) {
                partBadWords.add(s);
            } else {
                badWords.add(s);
            }
        }

    }

    public Fixture listOfBadWords() {
        return new WordFixture();
    }
}
