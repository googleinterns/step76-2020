// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package main.java.com.google.sps.notifs;

import com.google.api.services.gmail.Gmail;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.security.GeneralSecurityException;

public abstract class Notifier {

    private final String name;

    /** @param name String representing the name of person to be notified */
    public Notifier(String name) {
        this.name = name;
    }

    //    TODO(): Create a dummy email for ad lib itself to send emails.
    public abstract void notifyUser()
            throws MessagingException, IOException, GeneralSecurityException;

    /**
     * Getter function that returns the notifications recipient's name.
     *
     * @return String representing the notifications recipients name
     */
    public String getName() {
        return this.name;
    }
}
