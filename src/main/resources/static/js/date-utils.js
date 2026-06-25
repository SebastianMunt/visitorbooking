function subtractOneDay(dateString) {
    const date = parseDate(dateString);
    date.setDate(date.getDate() - 1);

    return formatDate(date);
}

function parseDate(dateString) {
    if (dateString instanceof Date) {
        return new Date(dateString);
    }

    const parts = dateString.split("-");
    return new Date(parts[0], parts[1] - 1, parts[2]);
}

function addDays(date, days) {
    const copy = new Date(date);
    copy.setDate(copy.getDate() + days);

    return copy;
}

function formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");

    return `${year}-${month}-${day}`;
}

function formatPrettyDate(dateInput) {
    const date = parseDate(dateInput);

    return date.toLocaleDateString("da-DK", {
        day: "numeric",
        month: "short"
    });
}

function formatFullPrettyDate(dateInput) {
    const date = parseDate(dateInput);

    return date.toLocaleDateString("da-DK", {
        day: "numeric",
        month: "short",
        year: "numeric"
    });
}