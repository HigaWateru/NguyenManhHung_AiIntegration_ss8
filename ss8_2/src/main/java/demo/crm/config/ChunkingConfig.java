package demo.crm.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChunkingConfig {

    @Bean
    public TextSplitter tokenBasedTextSplitter() {
        return TokenTextSplitter.builder()
            .withChunkSize(600)
            .withMinChunkSizeChars(120)
            .withMinChunkLengthToEmbed(20)
            .withMaxNumChunks(10_000)
            .withKeepSeparator(true)
            .build();
    }

    @Bean
    public TextSplitter headerBasedTextSplitter() {
        return new MarkdownHeaderTextSplitter(120);
    }

    private static final class MarkdownHeaderTextSplitter extends TextSplitter {
        private final int minChunkSizeChars;

        private MarkdownHeaderTextSplitter(int minChunkSizeChars) {
            this.minChunkSizeChars = minChunkSizeChars;
        }

        @Override
        protected List<String> splitText(String text) {
            String[] lines = text.replace("\r\n", "\n").split("\n", -1);
            List<String> sections = new ArrayList<>();
            StringBuilder current = new StringBuilder();

            for (String line : lines) {
                boolean isHeader = line.matches("^#{1,2}\\s+.+$");
                if (isHeader && current.length() > 0) {
                    sections.add(current.toString().trim());
                    current.setLength(0);
                }
                if (current.length() > 0) {
                    current.append('\n');
                }
                current.append(line);
            }
            if (current.length() > 0 && !current.toString().trim().isEmpty()) {
                sections.add(current.toString().trim());
            }

            return mergeShortSections(sections);
        }

        private List<String> mergeShortSections(List<String> sections) {
            List<String> merged = new ArrayList<>();
            for (String section : sections) {
                if (!merged.isEmpty() && section.length() < minChunkSizeChars) {
                    int previous = merged.size() - 1;
                    merged.set(previous, merged.get(previous) + "\n\n" + section);
                } else {
                    merged.add(section);
                }
            }
            return merged;
        }
    }
}
