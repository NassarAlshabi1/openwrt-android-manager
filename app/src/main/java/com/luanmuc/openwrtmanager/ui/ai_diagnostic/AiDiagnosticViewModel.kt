package com.luanmuc.openwrtmanager.ui.ai_diagnostic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.luanmuc.openwrtmanager.ui.base.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.data.repository.AiDiagnosticRepository
import com.luanmuc.openwrtmanager.util.Loc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A chat message shown in the AI diagnostic screen.
 */
data class AiChatMessage(
    val id: Long = System.currentTimeMillis(),
    val isFromUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * AI (Gemini) diagnostic ViewModel.
 *
 * Manages the chat, the API key setup and router-data collection.
 */
class AiDiagnosticViewModel(application: Application) : BaseViewModel(application) {

    private val repository = AiDiagnosticRepository.getInstance(application)

    // ==================== State ====================

    private val _messages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _isCollecting = MutableStateFlow(false)
    val isCollecting: StateFlow<Boolean> = _isCollecting.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _model = MutableStateFlow(com.luanmuc.openwrtmanager.data.api.GeminiClient.DEFAULT_MODEL)
    val model: StateFlow<String> = _model.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Whether router data will be attached to the next question. */
    private val _attachRouterData = MutableStateFlow(true)
    val attachRouterData: StateFlow<Boolean> = _attachRouterData.asStateFlow()

    init {
        initNetworkMonitor()
        _apiKey.value = repository.savedApiKey()
        _model.value = repository.savedModel()
    }

    val isConfigured: Boolean get() = _apiKey.value.isNotBlank()

    // ==================== Public actions ====================

    fun saveApiKey(key: String) {
        repository.saveApiKey(key)
        _apiKey.value = key.trim()
        if (key.isNotBlank() && _messages.value.isEmpty()) {
            appendMessage(AiChatMessage(isFromUser = false, text = Loc.s(R.string.ai_ready_hint)))
        }
    }

    fun saveModel(model: String) {
        repository.saveModel(model)
        _model.value = model.trim()
    }

    fun toggleAttachRouterData() {
        _attachRouterData.value = !_attachRouterData.value
    }

    fun clearError() {
        _error.value = null
    }

    fun clearChat() {
        _messages.value = emptyList()
    }

    /**
     * Full smart diagnosis: collect router data and ask Gemini to analyze it.
     */
    fun startSmartDiagnosis() {
        if (_isThinking.value || _isCollecting.value) return
        if (!ensureKey()) return

        viewModelScope.launch {
            var snapshotText = ""
            try {
                _isCollecting.value = true
                appendMessage(
                    AiChatMessage(isFromUser = false, text = Loc.s(R.string.ai_collecting_data))
                )
                val snapshot = repository.freshSnapshotText()
                snapshotText = snapshot
                _messages.value = _messages.value.dropLast(1) // remove "collecting..." placeholder
            } catch (e: Exception) {
                _messages.value = _messages.value.dropLast(1)
                _error.value = Loc.s(R.string.ai_err_collect, e.message ?: "")
                return@launch
            } finally {
                _isCollecting.value = false
            }

            val request = Loc.s(R.string.ai_diagnosis_request)
            appendMessage(AiChatMessage(isFromUser = true, text = request))

            _isThinking.value = true
            try {
                // First turn: diagnosis prompt + snapshot
                val answer = repository.runAiDiagnosisWithSnapshot(request, snapshotText)
                appendMessage(AiChatMessage(isFromUser = false, text = answer))
            } catch (e: Exception) {
                _error.value = Loc.s(R.string.ai_err_request, e.message ?: "")
            } finally {
                _isThinking.value = false
            }
        }
    }

    /**
     * Send a free-form follow-up question.
     */
    fun sendQuestion(question: String) {
        val q = question.trim()
        if (q.isEmpty() || _isThinking.value || _isCollecting.value) return
        if (!ensureKey()) return

        viewModelScope.launch {
            appendMessage(AiChatMessage(isFromUser = true, text = q))
            _isThinking.value = true
            try {
                val attach = _attachRouterData.value
                if (attach) {
                    _isCollecting.value = true
                }
                val answer = repository.askWithOptionalSnapshot(q, collectIfRequested = attach)
                appendMessage(AiChatMessage(isFromUser = false, text = answer))
            } catch (e: Exception) {
                _error.value = Loc.s(R.string.ai_err_request, e.message ?: "")
            } finally {
                _isCollecting.value = false
                _isThinking.value = false
            }
        }
    }

    // ==================== Internals ====================

    private fun ensureKey(): Boolean {
        if (isConfigured) return true
        _error.value = Loc.s(R.string.ai_err_no_key)
        return false
    }

    private fun appendMessage(message: AiChatMessage) {
        _messages.value = _messages.value + message
    }
}
