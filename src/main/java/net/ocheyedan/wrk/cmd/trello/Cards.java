package net.ocheyedan.wrk.cmd.trello;

import net.ocheyedan.wrk.Output;
import net.ocheyedan.wrk.RestTemplate;
import net.ocheyedan.wrk.cmd.Args;
import net.ocheyedan.wrk.cmd.Command;
import net.ocheyedan.wrk.cmd.Usage;
import net.ocheyedan.wrk.trello.Card;
import net.ocheyedan.wrk.trello.Label;
import net.ocheyedan.wrk.trello.TrelloUtil;
import org.codehaus.jackson.type.TypeReference;

import java.util.*;

/**
 * User: blangel
 * Date: 6/30/12
 * Time: 6:51 AM
 */
public final class Cards extends IdCommand {

    private final String url;

    private final String description;

    public Cards(Args args) {
        super(args);
        if ((args.args.size() == 2) && "in".equals(args.args.get(0))) {
            TrelloId boardId = parseWrkId(args.args.get(1), boardsPrefix);
            url = TrelloUtil.url("https://trello.com/1/boards/%s/cards?filter=open&key=%s&token=%s", boardId.id,
                    TrelloUtil.APP_DEV_KEY, TrelloUtil.USR_TOKEN);
            description = String.format("Open cards for board ^b^%s^r^:", boardId.id);
        } else if (args.args.isEmpty()) {
            url = TrelloUtil.url("https://trello.com/1/members/my/cards?filter=open&key=%s&token=%s", TrelloUtil.APP_DEV_KEY, TrelloUtil.USR_TOKEN);
            description = "Open cards assigned to you:";
        } else {
            url = description = null;
        }
    }

    @Override protected Map<String, String> _run() {
        if (url == null) {
            new Usage(args).run();
            return Collections.emptyMap();
        }
        Output.print(description);
        List<Card> cards = RestTemplate.get(url, new TypeReference<List<Card>>() {
        });
        if ((cards == null) || cards.isEmpty()) {
            Output.print("  ^black^None^r^");
            return Collections.emptyMap();
        }
        Map<String, String> wrkIds = new HashMap<String, String>(cards.size());
        int cardIndex = 1;
        for (Card card : cards) {
            String wrkId = "wrk" + cardIndex++;
            wrkIds.put(wrkId, String.format("c:%s", card.getId()));

            String labels = buildLabel(card.getLabels());
            Output.print("  ^b^%s^r^%s ^black^| %s^r^", card.getName(), labels, wrkId);
            Output.print("    ^black^%s^r^", getPrettyUrl(card));
        }
        return wrkIds;
    }

    /**
     * The {@link Card#getUrl()} returns a url based off of board and card's short-id; translating to long-id so that
     * users can copy and paste id printed via url.  Additionally removing the card name from the url to shorten the
     * length of the resulting url.
     * @param card to get long url
     * @return the long url (using {@link Card#getId()} instead of {@link Card#getIdShort()}
     */
    static String getPrettyUrl(Card card) {
        String originalUrl = card.getUrl();
        int firstIndex = originalUrl.indexOf("card/");
        if (firstIndex == -1) {
            return originalUrl; // balk
        }
        String toRemove = originalUrl.substring(firstIndex + 5);
        return originalUrl.replace(toRemove, card.getIdBoard() + "/" + card.getId());
    }

    static String buildLabel(List<Label> labels) {
        StringBuilder buffer = new StringBuilder();
        boolean colored = Output.isColoredOutput();
        for (Label label : labels) {
            String name = ((label.getName() == null) || label.getName().isEmpty()
                    ? (colored ? "  " : "[" + label.getColor() + "]")
                    : " " + label.getName() + " ");
            if ("green".equals(label.getColor())) {
                buffer.append(String.format(" ^i^^green^%s^r^", name));
            } else if ("yellow".equals(label.getColor())) {
                buffer.append(String.format(" ^i^^yellow^%s^r^", name));
            } else if ("orange".equals(label.getColor())) {
                buffer.append(String.format(" ^orange^^i^%s^r^", name));
            } else if ("red".equals(label.getColor())) {
                buffer.append(String.format(" ^i^^red^%s^r^", name));
            } else if ("purple".equals(label.getColor())) {
                buffer.append(String.format(" ^i^^magenta^%s^r^", name));
            } else if ("blue".equals(label.getColor())) {
                buffer.append(String.format(" ^i^^blue^%s^r^", name));
            }
        }
        return buffer.toString();
    }
}
