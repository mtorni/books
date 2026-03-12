(function () {

    const script = document.currentScript;
    const restaurantId = script?.dataset?.restaurantId || "";

    const API_URL = "/api/restaurant/ask";

    // floating button
    const button = document.createElement("button");
    button.innerHTML = "&#128172; Ask Us";
    button.style.position = "fixed";
    button.style.bottom = "20px";
    button.style.right = "20px";
    button.style.padding = "12px 16px";
    button.style.borderRadius = "20px";
    button.style.border = "none";
    button.style.background = "#333";
    button.style.color = "white";
    button.style.cursor = "pointer";
    button.style.zIndex = "9999";

    document.body.appendChild(button);

    // chat window
    const chat = document.createElement("div");
    chat.style.position = "fixed";
    chat.style.bottom = "70px";
    chat.style.right = "20px";
    chat.style.width = "320px";
    chat.style.height = "420px";
    chat.style.background = "white";
    chat.style.border = "1px solid #ccc";
    chat.style.borderRadius = "10px";
    chat.style.display = "none";
    chat.style.flexDirection = "column";
    chat.style.zIndex = "9999";
    chat.style.boxShadow = "0 4px 20px rgba(0,0,0,0.2)";

    chat.innerHTML = `
        <div style="padding:10px;background:#333;color:white;border-radius:10px 10px 0 0">
            Restaurant Assistant
            <span id="chatClose" style="float:right;cursor:pointer">✕</span>
        </div>

        <div id="chatMessages" style="flex:1;overflow:auto;padding:10px;font-size:14px"></div>

        <div style="padding:10px;border-top:1px solid #eee">
            <input id="chatInput" placeholder="Ask a question..."
                   style="width:70%;padding:6px"/>
            <button id="chatSend">Send</button>
        </div>
    `;

    document.body.appendChild(chat);

    const messages = chat.querySelector("#chatMessages");
    const input = chat.querySelector("#chatInput");
    const sendBtn = chat.querySelector("#chatSend");
    const closeBtn = chat.querySelector("#chatClose");

    function addMessage(sender, text) {
        const div = document.createElement("div");
        div.style.marginBottom = "8px";

        div.innerHTML = `<b>${sender}:</b> ${text}`;
        messages.appendChild(div);

        messages.scrollTop = messages.scrollHeight;
    }

    async function sendQuestion() {

        const q = input.value.trim();
        if (!q) return;

        addMessage("You", q);
        input.value = "";

        addMessage("Assistant", "Thinking...");

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

            const data = await response.json();

            messages.lastChild.innerHTML =
                `<b>Assistant:</b> ${data.answer}`;

        } catch (err) {

            messages.lastChild.innerHTML =
                `<b>Assistant:</b> Sorry, something went wrong.`;

        }
    }

    button.onclick = () => {
        chat.style.display = "flex";
        input.focus();
    };

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