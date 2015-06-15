/**
This file is part of CallRec.

    CallRec is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    CallRec is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with CallRec.  If not, see <http://www.gnu.org/licenses/>.
	*/

package com.artem.callrec;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

/*
 * Created by 805268 on 27.04.2015.
 */
public class DeviceAdminDemo extends DeviceAdminReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }

    public void onEnabled(Context context, Intent intent) {
    };

    public void onDisabled(Context context, Intent intent) {
    };
}