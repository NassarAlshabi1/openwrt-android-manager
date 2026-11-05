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
 * 路由器状态桌面小部件
 * 1x1 尺寸，显示路由器在线状态和基本信息
 */
class RouterStatusWidget : AppWidgetProvider() {
    
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
            val views = RemoteViews(context.packageName, R.layout.widget_router_status)
            
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = android.app.PendingIntent.getActivity(
                context, 0, intent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            
            views.setTextViewText(R.id.widget_router_name, Loc.s(R.string.s0954))
            views.setTextViewText(R.id.widget_status, Loc.s(R.string.s0955))
            views.setTextViewText(R.id.widget_info, "")
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
