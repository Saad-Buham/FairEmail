package eu.faircode.email;

/*
    This file is part of FairEmail.

    FairEmail is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FairEmail is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with FairEmail.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2018-2021 by Marcel Bokhorst (M66B)
*/

import android.app.Person;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.view.textclassifier.ConversationAction;
import android.view.textclassifier.ConversationActions;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TextHelper {
    private static final int MAX_SAMPLE_SIZE = 8192;
    private static final float MIN_PROBABILITY = 0.80f;

    static {
        System.loadLibrary("fairemail");
    }

    private static native DetectResult jni_detect_language(byte[] octets);

    static Locale detectLanguage(Context context, String text) {
        // Why not ML kit?
        // https://developers.google.com/ml-kit/terms

        if (TextUtils.isEmpty(text))
            return null;

        byte[] octets = text.getBytes();
        byte[] sample;
        if (octets.length < MAX_SAMPLE_SIZE)
            sample = octets;
        else {
            sample = new byte[MAX_SAMPLE_SIZE];
            System.arraycopy(octets, 0, sample, 0, MAX_SAMPLE_SIZE);
        }

        long start = new Date().getTime();
        Log.i("cld3 sample=" + sample.length);
        DetectResult result = jni_detect_language(sample);
        long elapse = new Date().getTime() - start;
        Log.i("cld3 language=" + result + " elapse=" + elapse);

        if (result.probability < MIN_PROBABILITY)
            return null;

        try {
            return Locale.forLanguageTag(result.language);
        } catch (Throwable ex) {
            Log.w(ex);
            return null;
        }
    }

    static ConversationActions getConversationActions(
            Context context, String[] texts, boolean replies, boolean outgoing, long time) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            return null;

        TextClassificationManager tcm =
                (TextClassificationManager) context.getSystemService(Context.TEXT_CLASSIFICATION_SERVICE);
        if (tcm == null)
            return null;

        Person author = outgoing
                ? ConversationActions.Message.PERSON_USER_SELF
                : ConversationActions.Message.PERSON_USER_OTHERS;
        ZonedDateTime dt = new Date(time)
                .toInstant()
                .atZone(ZoneId.systemDefault());
        List<ConversationActions.Message> input = new ArrayList<>();
        for (String text : texts)
            input.add(new ConversationActions.Message.Builder(author)
                    .setReferenceTime(dt)
                    .setText(text)
                    .build());

        Set<String> excluded = new HashSet<>(Arrays.asList(
                ConversationAction.TYPE_OPEN_URL,
                ConversationAction.TYPE_SEND_EMAIL
        ));
        if (!replies)
            excluded.add(ConversationAction.TYPE_TEXT_REPLY);
        TextClassifier.EntityConfig config =
                new TextClassifier.EntityConfig.Builder()
                        .setExcludedTypes(excluded)
                        .build();

        List<String> hints = Collections.unmodifiableList(Arrays.asList(
                ConversationActions.Request.HINT_FOR_IN_APP
        ));
        ConversationActions.Request request =
                new ConversationActions.Request.Builder(input)
                        .setTypeConfig(config)
                        .setHints(hints)
                        .build();

        return tcm.getTextClassifier().suggestConversationActions(request);
    }

    private static class DetectResult {
        String language;
        float probability;
        boolean is_reliable;
        float proportion;

        DetectResult(String language, float probability, boolean is_reliable, float proportion) {
            this.language = language;
            this.probability = probability;
            this.is_reliable = is_reliable;
            this.proportion = proportion;
        }

        @Override
        public String toString() {
            return language + " p=" + probability + " r=" + is_reliable + " pr=" + proportion;
        }
    }
}
