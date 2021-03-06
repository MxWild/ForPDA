package forpdateam.ru.forpda.apirx.apiclasses;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import biz.source_code.miniTemplator.MiniTemplator;
import forpdateam.ru.forpda.App;
import forpdateam.ru.forpda.R;
import forpdateam.ru.forpda.api.Api;
import forpdateam.ru.forpda.api.ApiUtils;
import forpdateam.ru.forpda.api.others.user.ForumUser;
import forpdateam.ru.forpda.api.theme.models.Poll;
import forpdateam.ru.forpda.api.theme.models.PollQuestion;
import forpdateam.ru.forpda.api.theme.models.PollQuestionItem;
import forpdateam.ru.forpda.api.theme.models.ThemePage;
import forpdateam.ru.forpda.api.theme.models.ThemePost;
import forpdateam.ru.forpda.apirx.ForumUsersCache;
import forpdateam.ru.forpda.client.ClientHelper;
import forpdateam.ru.forpda.common.Preferences;
import io.reactivex.Observable;

/**
 * Created by radiationx on 25.03.17.
 */

public class ThemeRx {
    private final static Pattern firstLetter = Pattern.compile("([a-zA-Zа-яА-Я])");


    public Observable<ThemePage> getTheme(final String url, boolean withHtml, boolean hatOpen, boolean pollOpen) {
        return Observable.fromCallable(() -> transform(Api.Theme().getTheme(url, hatOpen, pollOpen), withHtml));
    }

    public Observable<String> reportPost(int themeId, int postId, String message) {
        return Observable.fromCallable(() -> Api.Theme().reportPost(themeId, postId, message));
    }

    public Observable<Boolean> deletePost(int postId) {
        return Observable.fromCallable(() -> Api.Theme().deletePost(postId));
    }

    public Observable<String> votePost(int postId, boolean type) {
        return Observable.fromCallable(() -> Api.Theme().votePost(postId, type));
    }

    public static ThemePage transform(ThemePage page, boolean withHtml) throws Exception {
        if (withHtml) {

            List<ForumUser> forumUsers = new ArrayList<>();
            for (ThemePost post : page.getPosts()) {
                ForumUser forumUser = new ForumUser();
                forumUser.setId(post.getUserId());
                forumUser.setNick(post.getNick());
                forumUser.setAvatar(post.getAvatar());
            }
            ForumUsersCache.saveUsers(forumUsers);

            int memberId = ClientHelper.getUserId();
            MiniTemplator t = App.get().getTemplate(App.TEMPLATE_THEME);
            App.setTemplateResStrings(t);
            boolean authorized = ClientHelper.getAuthState();
            boolean prevDisabled = page.getPagination().getCurrent() <= 1;
            boolean nextDisabled = page.getPagination().getCurrent() == page.getPagination().getAll();

            t.setVariableOpt("style_type", App.get().getCssStyleType());

            t.setVariableOpt("topic_title", ApiUtils.htmlEncode(page.getTitle()));
            t.setVariableOpt("topic_description", ApiUtils.htmlEncode(page.getDesc()));
            t.setVariableOpt("topic_url", page.getUrl());

            t.setVariableOpt("all_pages_int", page.getPagination().getAll());
            t.setVariableOpt("posts_on_page_int", page.getPagination().getPerPage());
            t.setVariableOpt("current_page_int", page.getPagination().getCurrent());

            t.setVariableOpt("authorized_bool", Boolean.toString(authorized));
            t.setVariableOpt("is_curator_bool", Boolean.toString(page.isCurator()));
            t.setVariableOpt("member_id_int", ClientHelper.getUserId());
            t.setVariableOpt("elem_to_scroll", page.getAnchor());
            t.setVariableOpt("body_type", "topic");

            t.setVariableOpt("navigation_disable", getDisableStr(prevDisabled && nextDisabled));
            t.setVariableOpt("first_disable", getDisableStr(prevDisabled));
            t.setVariableOpt("prev_disable", getDisableStr(prevDisabled));
            t.setVariableOpt("next_disable", getDisableStr(nextDisabled));
            t.setVariableOpt("last_disable", getDisableStr(nextDisabled));

            t.setVariableOpt("in_favorite_bool", Boolean.toString(page.isInFavorite()));
            boolean isEnableAvatars = Preferences.Theme.isShowAvatars(null);
            t.setVariableOpt("enable_avatars_bool", Boolean.toString(isEnableAvatars));
            t.setVariableOpt("enable_avatars", isEnableAvatars ? "show_avatar" : "hide_avatar");
            t.setVariableOpt("avatar_type", Preferences.Theme.isCircleAvatars(null) ? "circle_avatar" : "square_avatar");


            int hatPostId = 0;
            if (!page.getPosts().isEmpty()) {
                hatPostId = page.getPosts().get(0).getId();
            }
            Matcher letterMatcher = null;
            for (ThemePost post : page.getPosts()) {
                t.setVariableOpt("user_online", post.isOnline() ? "online" : "");
                t.setVariableOpt("post_id", post.getId());
                t.setVariableOpt("user_id", post.getUserId());

                //Post header
                //t.setVariableOpt("avatar", post.getAvatar().isEmpty() ? "file:///android_asset/av.png" : "https://s.4pda.to/forum/uploads/".concat(post.getAvatar()));
                t.setVariableOpt("avatar", post.getAvatar());
                t.setVariableOpt("none_avatar", post.getAvatar().isEmpty() ? "none_avatar" : "");

                if (letterMatcher != null) {
                    letterMatcher = letterMatcher.reset(post.getNick());
                } else {
                    letterMatcher = firstLetter.matcher(post.getNick());
                }
                String letter = null;
                if (letterMatcher.find()) {
                    letter = letterMatcher.group(1);
                } else {
                    letter = post.getNick().substring(0, 1);
                }
                t.setVariableOpt("nick_letter", letter);
                t.setVariableOpt("nick", ApiUtils.htmlEncode(post.getNick()));
                t.setVariableOpt("curator", post.isCurator() ? "curator" : "");
                t.setVariableOpt("group_color", post.getGroupColor());
                t.setVariableOpt("group", post.getGroup());
                t.setVariableOpt("reputation", post.getReputation());
                t.setVariableOpt("date", post.getDate());
                t.setVariableOpt("number", post.getNumber());

                //Post body
                if (page.getPosts().size() > 1 && hatPostId == post.getId()) {
                    boolean hatOpened = prevDisabled || page.isHatOpen();
                    t.setVariableOpt("hat_state_class", prevDisabled || page.isHatOpen() ? "open" : "close");
                    //t.setVariableOpt("hat_body_state", prevDisabled || page.isHatOpen() ? "" : "hidden");
                    t.addBlockOpt("hat_button");
                    t.addBlockOpt("hat_content_start");
                    t.addBlockOpt("hat_content_end");
                } else {
                    t.setVariableOpt("hat_state_class", "");
                }
                t.setVariableOpt("body", post.getBody());

                //Post footer

                if (post.canReport() && authorized)
                    t.addBlockOpt("report_block");
                if (page.canQuote() && authorized && post.getUserId() != memberId)
                    t.addBlockOpt("reply_block");
                if (authorized && post.getUserId() != memberId)
                    t.addBlockOpt("vote_block");
                if (post.canDelete() && authorized)
                    t.addBlockOpt("delete_block");
                if (post.canEdit() && authorized)
                    t.addBlockOpt("edit_block");

                t.addBlockOpt("post");
            }

            //Poll block
            if (page.getPoll() != null) {
                t.setVariableOpt("poll_state_class", page.isPollOpen() ? "open" : "close");
                //t.setVariableOpt("poll_body_state", page.isPollOpen() ? "" : "hidden");
                Poll poll = page.getPoll();
                boolean isResult = poll.isResult();
                t.setVariableOpt("poll_type", isResult ? "result" : "default");
                t.setVariableOpt("poll_title", poll.getTitle().isEmpty() || poll.getTitle().equals("-") ? App.get().getString(R.string.poll) : poll.getTitle());

                for (PollQuestion question : poll.getQuestions()) {
                    t.setVariableOpt("question_title", question.getTitle());

                    for (PollQuestionItem questionItem : question.getQuestionItems()) {
                        t.setVariableOpt("question_item_title", questionItem.getTitle());

                        if (isResult) {
                            t.setVariableOpt("question_item_votes", questionItem.getVotes());
                            t.setVariableOpt("question_item_percent", Float.toString(questionItem.getPercent()));
                            t.addBlockOpt("poll_result_item");
                        } else {
                            t.setVariableOpt("question_item_type", questionItem.getType());
                            t.setVariableOpt("question_item_name", questionItem.getName());
                            t.setVariableOpt("question_item_value", questionItem.getValue());
                            t.addBlockOpt("poll_default_item");
                        }
                    }
                    t.addBlockOpt("poll_question_block");
                }
                t.setVariableOpt("poll_votes_count", poll.getVotesCount());
                if (poll.haveButtons()) {
                    if (poll.haveVoteButton())
                        t.addBlockOpt("poll_vote_button");
                    if (poll.haveShowResultsButton())
                        t.addBlockOpt("poll_show_results_button");
                    if (poll.haveShowPollButton())
                        t.addBlockOpt("poll_show_poll_button");
                    t.addBlockOpt("poll_buttons");
                }
                t.addBlockOpt("poll_block");
            }


            page.setHtml(t.generateOutput());
            t.reset();
        }

        /*final String veryLongString = page.getHtml();

        int maxLogSize = 1000;
        for (int i = 0; i <= veryLongString.length() / maxLogSize; i++) {
            int start = i * maxLogSize;
            int end = (i + 1) * maxLogSize;
            end = end > veryLongString.length() ? veryLongString.length() : end;
            Log.v("FORPDA_LOG", veryLongString.substring(start, end));
        }*/

        return page;
    }

    public static String getDisableStr(boolean b) {
        return b ? "disabled" : "";
    }

}
