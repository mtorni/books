(function () {

    const script = document.currentScript;

    const restaurantId = script?.dataset?.restaurantId || "";
    const title = script?.dataset?.title || "Restaurant";
    const color = script?.dataset?.color || "#333";

    const welcome =
        script?.dataset?.welcome ||
        `Welcome to ${title}! How can I help today?`;

    const baseUrl = new URL(script.src).origin;
    const API_URL = baseUrl + "/api/restaurant/ask";

    const isMobile = window.innerWidth <= 600;

    const launcher = document.createElement("div");
    launcher.style.position = "fixed";
    launcher.style.bottom = isMobile ? "14px" : "20px";
    launcher.style.right = isMobile ? "14px" : "20px";
    launcher.style.display = "flex";
    launcher.style.alignItems = "center";
    launcher.style.gap = "6px";
    launcher.style.zIndex = "9999";

    const closeLauncher = document.createElement("button");
    closeLauncher.textContent = "✕";
    closeLauncher.style.border = "none";
    closeLauncher.style.background = "#ddd";
    closeLauncher.style.color = "#333";
    closeLauncher.style.width = isMobile ? "28px" : "22px";
    closeLauncher.style.height = isMobile ? "28px" : "22px";
    closeLauncher.style.borderRadius = "50%";
    closeLauncher.style.cursor = "pointer";
    closeLauncher.style.fontSize = isMobile ? "14px" : "12px";
    closeLauncher.style.lineHeight = isMobile ? "28px" : "22px";
    closeLauncher.style.padding = "0";

    const button = document.createElement("button");
    button.textContent = "💬 Ask Us";
    button.style.padding = isMobile ? "14px 18px" : "12px 16px";
    button.style.borderRadius = "24px";
    button.style.border = "none";
    button.style.background = color;
    button.style.color = "white";
    button.style.cursor = "pointer";
    button.style.boxShadow = "0 4px 12px rgba(0,0,0,0.2)";
    button.style.fontSize = isMobile ? "16px" : "14px";
    button.style.fontWeight = "600";

    launcher.appendChild(closeLauncher);
    launcher.appendChild(button);
    document.body.appendChild(launcher);

    closeLauncher.onclick = () => {
        launcher.remove();
    };

    const chat = document.createElement("div");
    chat.style.position = "fixed";
    chat.style.background = "white";
    chat.style.border = "1px solid #ccc";
    chat.style.borderRadius = "12px";
    chat.style.display = "none";
    chat.style.flexDirection = "column";
    chat.style.zIndex = "9999";
    chat.style.boxShadow = "0 6px 24px rgba(0,0,0,0.2)";
    chat.style.overflow = "hidden";
    chat.style.boxSizing = "border-box";

    if (isMobile) {
        chat.style.top = "10px";
        chat.style.bottom = "10px";
        chat.style.right = "10px";
        chat.style.left = "10px";
        chat.style.width = "auto";
        chat.style.height = "auto";
        chat.style.maxHeight = "calc(100vh - 20px)";
    } else {
        chat.style.bottom = "70px";
        chat.style.right = "20px";
        chat.style.width = "340px";
        chat.style.height = "460px";
    }

    chat.innerHTML = `
        <div style="padding:12px;background:${color};color:white;font-weight:bold;display:flex;justify-content:space-between;align-items:center;">
            <span>${title} Assistant</span>
            <span id="chatClose" style="cursor:pointer;font-size:18px;">✕</span>
        </div>

        <div id="chatMessages" style="flex:1;overflow:auto;padding:12px;background:#f8f8f8;font-size:14px;"></div>

        <div style="padding:10px;border-top:1px solid #eee;display:flex;gap:8px;background:white;">
            <input id="chatInput" placeholder="Ask a question..."
                   style="flex:1;padding:10px;border:1px solid #ccc;border-radius:8px;outline:none;font-size:16px;" />
            <button id="chatSend"
                    style="padding:10px 14px;border:none;border-radius:8px;background:${color};color:white;cursor:pointer;font-weight:600;">
                Send
            </button>
        </div>
    `;

    document.body.appendChild(chat);

    const messages = chat.querySelector("#chatMessages");
    const input = chat.querySelector("#chatInput");
    const sendBtn = chat.querySelector("#chatSend");
    const closeBtn = chat.querySelector("#chatClose");

    function escapeHtml(text) {
        const div = document.createElement("div");
        div.textContent = text;
        return div.innerHTML;
    }

    function addMessage(sender, text) {
        const row = document.createElement("div");
        row.style.display = "flex";
        row.style.marginBottom = "10px";
        row.style.justifyContent = sender === "You" ? "flex-end" : "flex-start";

        const bubble = document.createElement("div");
        bubble.style.maxWidth = "80%";
        bubble.style.padding = "10px 12px";
        bubble.style.borderRadius = "14px";
        bubble.style.lineHeight = "1.4";
        bubble.style.wordWrap = "break-word";

        if (sender === "You") {
            bubble.style.background = color;
            bubble.style.color = "white";
        } else {
            bubble.style.background = "#e9e9e9";
            bubble.style.color = "#111";
        }

        bubble.innerHTML = escapeHtml(text);
        row.appendChild(bubble);
        messages.appendChild(row);
        messages.scrollTop = messages.scrollHeight;

        return bubble;
    }

    function addTypingIndicator() {
        const row = document.createElement("div");
        row.style.display = "flex";
        row.style.marginBottom = "10px";

        const bubble = document.createElement("div");
        bubble.style.background = "#e9e9e9";
        bubble.style.borderRadius = "14px";
        bubble.style.padding = "10px 14px";
        bubble.style.fontSize = "18px";

        bubble.innerHTML = `
            <span style="animation: blink 1s infinite;">●</span>
            <span style="animation: blink 1s infinite 0.2s;">●</span>
            <span style="animation: blink 1s infinite 0.4s;">●</span>
        `;

        row.appendChild(bubble);
        messages.appendChild(row);
        messages.scrollTop = messages.scrollHeight;

        return row;
    }

    const style = document.createElement("style");
    style.innerHTML = `
        @keyframes blink {
            0% {opacity:0.2;}
            50% {opacity:1;}
            100% {opacity:0.2;}
        }
    `;
    document.head.appendChild(style);

    let welcomeShown = false;

    function openChat() {
        chat.style.display = "flex";
        input.focus();

        if (!welcomeShown) {
            addMessage("Assistant", welcome);
            welcomeShown = true;
        }
    }

    async function sendQuestion() {
        const q = input.value.trim();
        if (!q) return;

        addMessage("You", q);
        input.value = "";

        const typingIndicator = addTypingIndicator();

        try {
            const response = await fetch(API_URL, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    question: q,
                    restaurantId: restaurantId
                })
            });

            typingIndicator.remove();

            if (!response.ok) {
                addMessage("Assistant", "Sorry, something went wrong.");
                return;
            }

            const data = await response.json();

            addMessage(
                "Assistant",
                data.answer || "Sorry, no answer returned."
            );

        } catch (err) {
            typingIndicator.remove();
            addMessage("Assistant", "Sorry, something went wrong.");
        }

        messages.scrollTop = messages.scrollHeight;
    }

    button.onclick = openChat;

    closeBtn.onclick = () => {
        chat.style.display = "none";
    };

    sendBtn.onclick = sendQuestion;

    input.addEventListener("keypress", function (e) {
        if (e.key === "Enter") {
            sendQuestion();
        }
    });

})();