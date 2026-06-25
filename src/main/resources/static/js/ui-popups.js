function createEventBubble() {
    if (document.getElementById("event-message-bubble")) {
        return;
    }

    const bubble = document.createElement("div");
    bubble.id = "event-message-bubble";

    bubble.innerHTML = `
        <div id="event-message-bubble-title"></div>
        <div id="event-message-bubble-date"></div>
    `;

    bubble.style.position = "fixed";
    bubble.style.zIndex = "9999";
    bubble.style.maxWidth = "min(280px, calc(100vw - 32px))";
    bubble.style.padding = "12px 14px";
    bubble.style.borderRadius = "22px";
    bubble.style.background = "#28a745";
    bubble.style.color = "#ffffff";
    bubble.style.boxShadow = "0 12px 30px rgba(0, 0, 0, 0.22)";
    bubble.style.fontFamily = '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif';
    bubble.style.fontSize = "0.95rem";
    bubble.style.lineHeight = "1.25";
    bubble.style.wordBreak = "break-word";
    bubble.style.opacity = "0";
    bubble.style.pointerEvents = "none";
    bubble.style.transform = "translateY(8px) scale(0.96)";
    bubble.style.transition = "opacity 160ms ease, transform 160ms ease";
    bubble.style.display = "none";

    document.body.appendChild(bubble);

    const titleElement = document.getElementById("event-message-bubble-title");
    const dateElement = document.getElementById("event-message-bubble-date");

    titleElement.style.fontWeight = "800";
    titleElement.style.marginBottom = "4px";

    dateElement.style.fontSize = "0.78rem";
    dateElement.style.opacity = "0.88";
    dateElement.style.fontWeight = "650";
}

function showEventBubble(title, dateText, mouseEvent, type, comment, days) {
    const bubble = document.getElementById("event-message-bubble");
    const titleElement = document.getElementById("event-message-bubble-title");
    const dateElement = document.getElementById("event-message-bubble-date");

    if (!bubble || !titleElement || !dateElement) {
        return;
    }

    titleElement.textContent = title;

    let details = dateText;

    if (days) {
        details += ` · ${days} dag${days > 1 ? "e" : ""}`;
    }

    if (type) {
        details += ` · ${type}`;
    }

    if (comment && comment.trim() !== "") {
        details += `\n${comment}`;
    }

    dateElement.textContent = details;
    dateElement.style.whiteSpace = "pre-line";

    bubble.style.display = "block";
    bubble.style.opacity = "0";
    bubble.style.pointerEvents = "auto";
    bubble.style.transform = "translateY(8px) scale(0.96)";

    const bubbleWidth = Math.min(300, window.innerWidth - 32);
    let left = mouseEvent.clientX - bubbleWidth / 2;
    let top = mouseEvent.clientY - 92;

    if (left < 16) {
        left = 16;
    }

    if (left + bubbleWidth > window.innerWidth - 16) {
        left = window.innerWidth - bubbleWidth - 16;
    }

    if (top < 84) {
        top = mouseEvent.clientY + 18;
    }

    bubble.style.width = bubbleWidth + "px";
    bubble.style.left = left + "px";
    bubble.style.top = top + "px";

    requestAnimationFrame(function () {
        bubble.style.opacity = "1";
        bubble.style.transform = "translateY(0) scale(1)";
    });
}

function hideEventBubble() {
    const bubble = document.getElementById("event-message-bubble");

    if (!bubble) {
        return;
    }

    bubble.style.opacity = "0";
    bubble.style.pointerEvents = "none";
    bubble.style.transform = "translateY(8px) scale(0.96)";

    setTimeout(function () {
        if (bubble.style.opacity === "0") {
            bubble.style.display = "none";
        }
    }, 180);
}

function getEventDateText(event) {
    const startText = formatPrettyDate(event.start);

    if (!event.end) {
        return startText;
    }

    const realEndDate = subtractOneDay(formatDate(event.end));
    const endText = formatPrettyDate(realEndDate);

    if (endText === startText) {
        return startText;
    }

    return `${startText} til ${endText}`;
}

function setupBookingSuccessPopup() {
    const popup = document.getElementById("booking-success-popup");

    if (!popup) {
        return;
    }

    popup.addEventListener("click", function () {
        popup.classList.add("is-hidden");

        setTimeout(function () {
            popup.remove();
        }, 200);
    });
}