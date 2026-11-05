package com.luanmuc.openwrtmanager.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.luanmuc.openwrtmanager.MainActivity
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 网速桌面小部件
 * 2x2 尺寸，显示实时上传下载速度
 */
class NetworkSpeedWidget : AppWidgetProvider() {
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_network_speed)
            
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = android.app.PendingIntent.getActivity(
                context, 0, intent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            
            views.setTextViewText(R.id.widget_download_speed, "--")
            views.setTextViewText(R.id.widget_upload_speed, "--")
            views.setTextViewText(R.id.widget_router_name, Loc.s(R.string.s0953))
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
