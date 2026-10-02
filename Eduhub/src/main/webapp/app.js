// =============================================================================
// EduFinance Hub - Master Frontend Application Engine
// File: src/main/webapp/js/app.js
// Package Namespace: za.ac.cput.eduhub
// =============================================================================

function getApiUrl(endpoint) {
    const cleanEndpoint = endpoint.startsWith("/") ? endpoint.substring(1) : endpoint;
    const parts = window.location.pathname.split("/").filter(Boolean);
    const contextPath = (parts.length > 0 && !parts[0].includes(".html")) ? `/${parts[0]}` : "";
    return `${contextPath}/${cleanEndpoint}`;
}

function showTopError(message, returnToUrl) {
    const alertBox = document.getElementById("stickyTopAlert");
    if (!alertBox) {
        alert(message);
        return;
    }
    const msgSpan = alertBox.querySelector(".error-msg-text");
    if (msgSpan) {
        msgSpan.innerHTML = "";
        const warnIcon = document.createElement("i");
        warnIcon.className = "fa-solid fa-triangle-exclamation";
        warnIcon.setAttribute("aria-hidden", "true");
        msgSpan.appendChild(warnIcon);
        msgSpan.appendChild(document.createTextNode(` ${message}`));
    }
    alertBox.classList.add("visible");
    window.scrollTo({ top: 0, behavior: "smooth" });

    if (returnToUrl) {
        setTimeout(() => {
            window.location.href = returnToUrl;
        }, 2500);
    }
}

function dismissTopError() {
    const alertBox = document.getElementById("stickyTopAlert");
    if (alertBox) {
        alertBox.classList.remove("visible");
    }
}

function validateFormAndBounce(formElement) {
    dismissTopError();
    const inputs = formElement.querySelectorAll("input, select, textarea");
    let hasMissing = false;
    let firstMissingName = "";

    inputs.forEach(input => {
        input.classList.remove("has-error");
        const isOptional = input.hasAttribute("data-optional") ||
            (input.getAttribute("placeholder") && input.getAttribute("placeholder").toLowerCase().includes("optional"));

        if (!isOptional && input.hasAttribute("required") && !input.value.trim()) {
            input.classList.add("has-error");
            if (!hasMissing) {
                hasMissing = true;
                const label = formElement.querySelector(`label[for="${input.id}"]`);
                firstMissingName = label ? label.textContent.replace("*", "").trim() : input.name || "Required field";
            }
        }
    });

    if (hasMissing) {
        showTopError(`Missing information: Please fill in "${firstMissingName}" to continue.`);
        return false;
    }
    return true;
}

function calculateAcademicResults() {
    const rows = document.querySelectorAll("#dynamicMarksRowsList .mark-row-item");
    let totalMarks = 0;
    let count = 0;
    let hasError = false;

    rows.forEach(r => {
        const subInput = r.querySelector(".subject-input");
        const scoreInput = r.querySelector(".score-input");
        const sub = subInput ? subInput.value.trim() : "";
        const score = scoreInput ? parseFloat(scoreInput.value) : NaN;

        if (sub || !isNaN(score)) {
            if (!sub || isNaN(score) || score < 0 || score > 100) {
                if (scoreInput) scoreInput.classList.add("has-error");
                hasError = true;
            } else {
                if (scoreInput) scoreInput.classList.remove("has-error");
                totalMarks += score;
                count++;
            }
        }
    });

    if (hasError) {
        showTopError("Invalid information: All entered subjects must have valid marks ranging from 0% to 100%.");
        return 0;
    }

    if (count === 0) {
        showTopError("Missing information: Please add at least one subject and its marks percentage.");
        return 0;
    }

    const average = Math.round(totalMarks / count);

    const circle = document.getElementById("donutStrokeAnimated");
    const label = document.getElementById("calculatedPercentageLabel");
    if (circle && label) {
        const radius = circle.r.baseVal.value;
        const circumference = 2 * Math.PI * radius;
        circle.style.strokeDasharray = `${circumference} ${circumference}`;
        const offset = circumference - (average / 100) * circumference;
        circle.style.strokeDashoffset = offset;
        label.textContent = `${average}%`;
    }
    return average;
}

function appendDynamicMarkRow() {
    const container = document.getElementById("dynamicMarksRowsList");
    if (!container) return;

    const row = document.createElement("div");
    row.className = "mark-row-item";

    const subInput = document.createElement("input");
    subInput.type = "text";
    subInput.className = "form-input subject-input";
    subInput.placeholder = "Subject Name";

    const scoreInput = document.createElement("input");
    scoreInput.type = "number";
    scoreInput.className = "form-input score-input";
    scoreInput.placeholder = "Marks%";
    scoreInput.min = "0";
    scoreInput.max = "100";

    row.appendChild(subInput);
    row.appendChild(scoreInput);
    container.appendChild(row);
}

async function searchLiveBursaries() {
    const feed = document.getElementById("liveBursaryFeed");
    if (!feed) return;
    const searchBox = document.getElementById("bursarySearchBox");
    const q = searchBox ? searchBox.value.trim() : "";

    try {
        const res = await fetch(getApiUrl(`api/bursaries/search?q=${encodeURIComponent(q)}`));
        if (res.status === 403) {
            feed.textContent = "";
            const lockCard = document.createElement("div");
            lockCard.className = "card card-amber";

            const lockTitle = document.createElement("strong");
            lockTitle.className = "card-feature-title";
            lockTitle.textContent = "Bursary Matching Locked";

            const lockDesc = document.createElement("p");
            lockDesc.className = "card-feature-desc";
            lockDesc.textContent = "Complete your 4-step Bursary Quest onboarding profile to unlock matching against the 200+ South African schemes.";

            const lockLink = document.createElement("a");
            lockLink.href = "wf05_bursary_quest.html";
            lockLink.className = "btn-primary";
            lockLink.textContent = "Start Bursary Quest";

            lockCard.appendChild(lockTitle);
            lockCard.appendChild(lockDesc);
            lockCard.appendChild(lockLink);
            feed.appendChild(lockCard);
            return;
        }

        const rawText = await res.text();
        let bursaries = [];
        try {
            bursaries = JSON.parse(rawText);
        } catch (e) {
            feed.innerHTML = `<div class="card text-center error-msg-text">Data error: ${rawText.substring(0, 100)}</div>`;
            return;
        }

        feed.textContent = "";

        if (!Array.isArray(bursaries) || bursaries.length === 0) {
            const emptyMsg = document.createElement("div");
            emptyMsg.className = "card text-center";
            emptyMsg.textContent = "No bursaries matched your search criteria. Try a broader keyword.";
            feed.appendChild(emptyMsg);
            return;
        }

        bursaries.forEach(b => {
            const card = document.createElement("article");
            card.className = "card card-blue card-clickable bursary-list-item";

            const headerRow = document.createElement("div");
            headerRow.className = "item-header-row";

            const catBadge = document.createElement("span");
            catBadge.className = "badge badge-purple";
            catBadge.textContent = b.category || "General";

            const deadlineSpan = document.createElement("span");
            deadlineSpan.className = "deadline-callout";
            deadlineSpan.textContent = `Deadline: ${b.closingDeadline || "Open"}`;

            headerRow.appendChild(catBadge);
            headerRow.appendChild(deadlineSpan);

            const titleHeading = document.createElement("h3");
            titleHeading.className = "card-title";
            titleHeading.textContent = b.title;

            const orgDesc = document.createElement("div");
            orgDesc.className = "organization-name-sub";
            orgDesc.textContent = b.organization;

            const fundingDesc = document.createElement("div");
            fundingDesc.className = "funding-value-highlight";
            fundingDesc.textContent = `Amount: ${b.fundingAmountDesc}`;

            const chipsRow = document.createElement("div");
            chipsRow.className = "chip-grid";

            const minMarkChip = document.createElement("span");
            minMarkChip.className = "chip compact-chip";
            minMarkChip.textContent = `Min Mark: ${b.minAcademicPercentage}%`;

            const fieldChip = document.createElement("span");
            fieldChip.className = "chip compact-chip";
            fieldChip.textContent = `Field: ${b.targetField}`;

            chipsRow.appendChild(minMarkChip);
            chipsRow.appendChild(fieldChip);

            const applyLink = document.createElement("a");
            applyLink.href = `wf12_bursary_details.html?id=${b.bursaryId}`;
            applyLink.className = "btn-secondary compact-btn align-self-end";
            applyLink.textContent = "View & Apply";

            card.appendChild(headerRow);
            card.appendChild(titleHeading);
            card.appendChild(orgDesc);
            card.appendChild(fundingDesc);
            card.appendChild(chipsRow);
            card.appendChild(applyLink);

            feed.appendChild(card);
        });
    } catch (e) {
        console.error("Bursary search failed:", e);
    }
}

async function loadVaultDashboard() {
    const list = document.getElementById("vaultDocsList");
    if (!list) return;

    try {
        const res = await fetch(getApiUrl("api/documents"));
        if (res.ok) {
            const data = await res.json();
            const docs = data.documents || [];
            list.textContent = "";

            const completionBadge = document.getElementById("vaultCompletionBadge");
            const progressBar = document.getElementById("vaultProgressBar");
            const countSubtext = document.getElementById("vaultCountSubtext");

            if (completionBadge) completionBadge.textContent = `${data.completionPercentage}% Completed`;
            if (progressBar) progressBar.style.width = `${data.completionPercentage}%`;
            if (countSubtext) countSubtext.textContent = `${data.verifiedCount} of ${data.totalRequired} required documents verified`;

            if (docs.length === 0) {
                const emptyCard = document.createElement("div");
                emptyCard.className = "card text-center";
                emptyCard.textContent = "Your document vault is currently empty. Upload files to verify eligibility.";
                list.appendChild(emptyCard);
                return;
            }

            docs.forEach(doc => {
                const item = document.createElement("div");
                item.className = "vault-doc-row";

                const leftInfo = document.createElement("div");
                leftInfo.className = "vault-doc-info";

                const icon = document.createElement("div");
                icon.className = "vault-icon-badge";
                const docIcon = document.createElement("i");
                docIcon.className = "fa-solid fa-file-lines";
                docIcon.setAttribute("aria-hidden", "true");
                icon.appendChild(docIcon);

                const textGroup = document.createElement("div");
                const titleStrong = document.createElement("strong");
                titleStrong.textContent = doc.documentType;

                const metaSpan = document.createElement("span");
                metaSpan.className = "subtext-muted";
                metaSpan.textContent = doc.fileName;

                textGroup.appendChild(titleStrong);
                textGroup.appendChild(metaSpan);
                leftInfo.appendChild(icon);
                leftInfo.appendChild(textGroup);

                const statusBadge = document.createElement("span");
                statusBadge.className = "badge badge-green";
                statusBadge.textContent = doc.verificationStatus || "Verified";

                item.appendChild(leftInfo);
                item.appendChild(statusBadge);
                list.appendChild(item);
            });
        }
    } catch (e) {
        console.error("Vault data fetch error:", e);
    }
}

async function loadWalletData() {
    const balanceElem = document.getElementById("walletBalance");
    if (!balanceElem) return;

    try {
        const res = await fetch(getApiUrl("api/wallet/dashboard"));
        if (res.ok) {
            const data = await res.json();
            balanceElem.textContent = `R${parseFloat(data.availableBalance || 0).toLocaleString()}`;

            const incomeElem = document.getElementById("walletIncome");
            if (incomeElem) incomeElem.textContent = `R${parseFloat(data.monthlyIncome || 0).toLocaleString()}`;

            const expensesElem = document.getElementById("walletExpenses");
            if (expensesElem) expensesElem.textContent = `R${parseFloat(data.monthlyExpenses || 0).toLocaleString()}`;

            const savingsElem = document.getElementById("walletSavings");
            if (savingsElem) {
                const savings = parseFloat(data.monthlyIncome || 0) - parseFloat(data.monthlyExpenses || 0);
                savingsElem.textContent = `R${savings.toLocaleString()}`;
            }

            renderRealtimeDonutChart(data.distribution || []);
            renderWeeklySpendBars(data.weeklySpending || []);
        }
    } catch (e) {
        console.error("Wallet loading error:", e);
    }

    if (document.getElementById("categoryCardsContainer")) {
        loadBudgetCategories();
    }
    if (document.getElementById("trendsLineChart")) {
        loadWalletTrends();
    }
}

async function loadBudgetCategories() {
    const container = document.getElementById("categoryCardsContainer");
    const emptyMsg = document.getElementById("noCategoriesMessage");
    const overNotice = document.getElementById("overBudgetNotice");
    const overMsg = document.getElementById("overBudgetMessage");
    if (!container) return;

    try {
        const res = await fetch(getApiUrl("api/wallet/categories"));
        if (!res.ok) return;
        const categories = await res.json();

        container.textContent = "";

        if (categories.length === 0) {
            if (emptyMsg) emptyMsg.classList.remove("is-hidden");
            if (overNotice) overNotice.classList.add("is-hidden");
            return;
        }
        if (emptyMsg) emptyMsg.classList.add("is-hidden");

        const overBudgetNames = [];

        categories.forEach(cat => {
            const limit = parseFloat(cat.limit || 0);
            const spent = parseFloat(cat.spent || 0);
            const pct = limit > 0 ? Math.min(100, Math.round((spent / limit) * 100)) : 0;
            const isOver = !!cat.isOver;
            if (isOver) overBudgetNames.push(cat.category);

            const card = document.createElement("div");
            card.className = "card category-card-item" + (isOver ? " category-over-limit" : "");

            const header = document.createElement("div");
            header.className = "category-card-header";
            const name = document.createElement("strong");
            name.textContent = cat.category;
            const amounts = document.createElement("span");
            amounts.textContent = `R${spent.toFixed(2)} / R${limit.toFixed(2)}`;
            header.appendChild(name);
            header.appendChild(amounts);

            const barTrack = document.createElement("div");
            barTrack.className = "category-progress-track";
            const barFill = document.createElement("div");
            barFill.className = "category-progress-fill" + (isOver ? " is-over" : "");
            barFill.style.width = `${pct}%`;
            barTrack.appendChild(barFill);

            card.appendChild(header);
            card.appendChild(barTrack);
            container.appendChild(card);
        });

        if (overNotice) {
            if (overBudgetNames.length > 0) {
                if (overMsg) overMsg.textContent = `${overBudgetNames.join(", ")} ${overBudgetNames.length === 1 ? "has" : "have"} exceeded the budget limit.`;
                overNotice.classList.remove("is-hidden");
            } else {
                overNotice.classList.add("is-hidden");
            }
        }
    } catch (e) {
        console.error("Budget categories fetch error:", e);
    }
}

async function loadWalletTrends() {
    const svg = document.getElementById("trendsLineChart");
    const emptyMsg = document.getElementById("trendsEmptyMessage");
    if (!svg) return;

    try {
        const res = await fetch(getApiUrl("api/wallet/trends"));
        if (!res.ok) return;
        const months = await res.json();
        renderTrendsChart(months, svg, emptyMsg);
    } catch (e) {
        console.error("Wallet trends fetch error:", e);
    }
}

function renderTrendsChart(months, svg, emptyMsg) {
    svg.textContent = "";

    const hasAnyData = months.some(m =>
        parseFloat(m.income || 0) > 0 || parseFloat(m.expenses || 0) > 0);

    if (!hasAnyData) {
        if (emptyMsg) emptyMsg.classList.remove("is-hidden");
        return;
    }
    if (emptyMsg) emptyMsg.classList.add("is-hidden");

    const chartLeft = 40, chartRight = 380, chartTop = 20, chartBottom = 180;
    const allValues = months.flatMap(m => [
        parseFloat(m.income || 0), parseFloat(m.expenses || 0), parseFloat(m.savings || 0)
    ]);
    const maxVal = Math.max(1, ...allValues);

    // Gridlines
    [0, 0.25, 0.5, 0.75, 1].forEach(frac => {
        const y = chartBottom - frac * (chartBottom - chartTop);
        const line = document.createElementNS("http://www.w3.org/2000/svg", "line");
        line.setAttribute("x1", chartLeft); line.setAttribute("x2", chartRight);
        line.setAttribute("y1", y); line.setAttribute("y2", y);
        line.setAttribute("stroke", "#E2DEFB");
        line.setAttribute("stroke-width", "1");
        if (frac !== 0) line.setAttribute("stroke-dasharray", "2");
        svg.appendChild(line);
    });

    const xStep = (chartRight - chartLeft) / Math.max(1, months.length - 1);

    function buildPath(key, color) {
        const points = months.map((m, i) => {
            const x = chartLeft + i * xStep;
            const val = parseFloat(m[key] || 0);
            const y = chartBottom - (val / maxVal) * (chartBottom - chartTop);
            return `${x} ${y}`;
        });
        const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
        path.setAttribute("d", "M" + points.join(" L "));
        path.setAttribute("fill", "none");
        path.setAttribute("stroke", color);
        path.setAttribute("stroke-width", "3");
        svg.appendChild(path);
    }

    buildPath("income", "#7C5CFC");
    buildPath("expenses", "#E63946");
    buildPath("savings", "#2EC4B6");

    // Month labels along the x-axis
    months.forEach((m, i) => {
        const x = chartLeft + i * xStep;
        const text = document.createElementNS("http://www.w3.org/2000/svg", "text");
        text.setAttribute("x", x);
        text.setAttribute("y", chartBottom + 16);
        text.setAttribute("font-size", "10");
        text.setAttribute("text-anchor", "middle");
        text.setAttribute("fill", "#6C5297");
        text.textContent = m.label;
        svg.appendChild(text);
    });
}

function renderWeeklySpendBars(weeklyData) {
    const container = document.getElementById("weeklySpendBarsContainer");
    if (!container) return;
    container.textContent = "";

    const data = weeklyData && weeklyData.length === 7
        ? weeklyData
        : ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"].map(day => ({ day, amount: 0 }));

    const maxAmount = Math.max(1, ...data.map(d => parseFloat(d.amount || 0)));

    data.forEach(d => {
        const amount = parseFloat(d.amount || 0);
        const heightPct = Math.max(2, Math.round((amount / maxAmount) * 100));

        const col = document.createElement("div");
        col.className = "bar-column";
        col.title = `${d.day}: R${amount.toFixed(2)}`;

        const bar = document.createElement("div");
        bar.className = "bar-fill";
        bar.style.height = `${heightPct}%`;

        const label = document.createElement("span");
        label.className = "bar-day-label";
        label.textContent = d.day;

        col.appendChild(bar);
        col.appendChild(label);
        container.appendChild(col);
    });
}

let activeCalendarDate = new Date();

function renderRealtimeDonutChart(distributionList) {
    const svg = document.getElementById("walletDonutSvg");
    const legend = document.getElementById("walletCategoryLegend");
    if (!svg || !legend) return;

    svg.textContent = "";
    legend.textContent = "";

    if (distributionList.length === 0) {
        legend.innerHTML = "<div class='subtext-muted'>No expenses recorded. Log entries to generate chart.</div>";
        return;
    }

    const totalSpend = distributionList.reduce((acc, curr) => acc + parseFloat(curr.amount), 0);
    const colors = ["#E63946", "#2EC4B6", "#FFB703", "#7C5CFC", "#3A86FF", "#8338EC"];
    let accumulatedAngle = 0;

    distributionList.forEach((item, index) => {
        const amount = parseFloat(item.amount);
        const sliceAngle = (amount / totalSpend) * 360;
        const color = colors[index % colors.length];

        const circle = document.createElementNS("http://www.w3.org/2000/svg", "circle");
        circle.setAttribute("cx", "50");
        circle.setAttribute("cy", "50");
        circle.setAttribute("r", "35");
        circle.setAttribute("fill", "transparent");
        circle.setAttribute("stroke", color);
        circle.setAttribute("stroke-width", "16");

        const circumference = 2 * Math.PI * 35;
        const strokeLength = (sliceAngle / 360) * circumference;
        circle.setAttribute("stroke-dasharray", `${strokeLength} ${circumference}`);
        circle.setAttribute("stroke-dashoffset", -((accumulatedAngle / 360) * circumference));

        svg.appendChild(circle);
        accumulatedAngle += sliceAngle;

        const legendRow = document.createElement("div");
        legendRow.className = "legend-row";

        const dot = document.createElement("span");
        dot.className = "legend-dot";
        dot.style.backgroundColor = color;

        const label = document.createElement("span");
        const percentage = Math.round((amount / totalSpend) * 100);
        label.textContent = `${item.category} (${percentage}%)`;

        const val = document.createElement("strong");
        val.textContent = `R${amount.toFixed(2)}`;

        legendRow.appendChild(dot);
        legendRow.appendChild(label);
        legendRow.appendChild(val);
        legend.appendChild(legendRow);
    });
}

async function renderLiveCalendar(dateContext) {
    const grid = document.getElementById("dynamicCalendarGrid");
    const label = document.getElementById("calendarMonthYearDisplay");
    if (!grid || !label) return;

    grid.textContent = "";
    const year = dateContext.getFullYear();
    const month = dateContext.getMonth();
    const monthNames = ["JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
        "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"];
    label.textContent = `${monthNames[month]} ${year}`;

    let eventsList = [];
    try {
        const res = await fetch(getApiUrl("api/calendar"));
        if (res.ok) {
            eventsList = await res.json();
        }
    } catch (e) {
        console.warn("Could not retrieve calendar entries:", e);
    }

    const firstDayIndex = new Date(year, month, 1).getDay();
    const daysInCurrentMonth = new Date(year, month + 1, 0).getDate();
    const daysInPrevMonth = new Date(year, month, 0).getDate();

    for (let i = firstDayIndex; i > 0; i--) {
        const cell = document.createElement("div");
        cell.className = "cal-cell cal-cell-muted";
        cell.textContent = daysInPrevMonth - i + 1;
        grid.appendChild(cell);
    }

    for (let day = 1; day <= daysInCurrentMonth; day++) {
        const cell = document.createElement("div");
        cell.className = "cal-cell cal-cell-active";

        const dateSpan = document.createElement("span");
        dateSpan.textContent = day;
        cell.appendChild(dateSpan);

        const isoDate = `${year}-${String(month + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
        const matchedEvents = eventsList.filter(e => e.date === isoDate);

        if (matchedEvents.length > 0) {
            matchedEvents.forEach(ev => {
                const pill = document.createElement("div");
                pill.className = `event-pill ${ev.type === "Deadline" ? "pill-red" : "pill-green"}`;
                pill.textContent = ev.title;
                cell.appendChild(pill);
            });
        }

        cell.addEventListener("click", () => {
            document.querySelectorAll(".cal-cell").forEach(c => c.classList.remove("selected-date"));
            cell.classList.add("selected-date");
            displayUpcomingEventsFeed(matchedEvents);
        });

        grid.appendChild(cell);
    }
}

function displayUpcomingEventsFeed(events) {
    const feed = document.getElementById("calendarFeedList");
    if (!feed) return;
    feed.textContent = "";

    if (!events || events.length === 0) {
        feed.innerHTML = "<div class='subtext-muted'>No events scheduled on this date.</div>";
        return;
    }

    events.forEach(ev => {
        const row = document.createElement("div");
        row.className = "agenda-event-row";

        const icon = document.createElement("div");
        icon.className = "agenda-icon-lead";
        const calIcon = document.createElement("i");
        calIcon.className = "fa-solid fa-calendar-day";
        calIcon.setAttribute("aria-hidden", "true");
        icon.appendChild(calIcon);

        const textGroup = document.createElement("div");
        const titleStrong = document.createElement("strong");
        titleStrong.textContent = ev.title;

        const dateSub = document.createElement("span");
        dateSub.className = "subtext-muted";
        dateSub.textContent = `${ev.date} • ${ev.type || "Schedule"}`;

        textGroup.appendChild(titleStrong);
        textGroup.appendChild(dateSub);
        row.appendChild(icon);
        row.appendChild(textGroup);
        feed.appendChild(row);
    });
}

document.addEventListener("DOMContentLoaded", () => {
    const page = document.body.dataset.page || "";

    const dismissAlertBtn = document.getElementById("dismissTopAlertBtn");
    if (dismissAlertBtn) {
        dismissAlertBtn.addEventListener("click", dismissTopError);
    }

    const backBtn = document.getElementById("navBackBtn");
    if (backBtn) {
        backBtn.addEventListener("click", () => history.back());
    }

    document.querySelectorAll(".chip-grid").forEach(group => {
        const hiddenInput = group.parentElement.querySelector("input[type='hidden']");
        group.querySelectorAll(".chip").forEach(btn => {
            btn.addEventListener("click", () => {
                group.querySelectorAll(".chip").forEach(c => c.classList.remove("selected"));
                btn.classList.add("selected");
                if (hiddenInput) {
                    hiddenInput.value = btn.dataset.val || btn.textContent.trim();
                }
            });
        });
    });

    const registerForm = document.getElementById("createAccountForm");
    if (registerForm) {
        registerForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            if (!validateFormAndBounce(registerForm)) return;

            const fullName = (document.getElementById("regFullName")?.value || registerForm.elements["fullName"]?.value || "").trim();
            const email = (document.getElementById("regEmail")?.value || registerForm.elements["email"]?.value || "").trim().toLowerCase();
            const phone = (document.getElementById("regPhone")?.value || registerForm.elements["phone"]?.value || "").trim();
            const studentType = (document.getElementById("regStudentType")?.value || registerForm.elements["studentType"]?.value || "").trim();
            const password = document.getElementById("regPassword")?.value || registerForm.elements["password"]?.value || "";
            const confirm = document.getElementById("regConfirmPassword")?.value || registerForm.elements["confirmPassword"]?.value || "";

            if (!fullName) { showTopError("Missing information: Please enter your Full Name."); return; }
            if (!email) { showTopError("Missing information: Please enter your Email Address."); return; }
            if (!phone) { showTopError("Missing information: Please enter your Contact Phone Number."); return; }
            if (!studentType) { showTopError("Missing information: Please select your Student Type."); return; }
            if (!password) { showTopError("Missing information: Please create a password."); return; }

            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(email)) {
                showTopError("Invalid information: Please enter a valid email address.");
                return;
            }

            const phoneClean = phone.replace(/\s+/g, "");
            const phoneRegex = /^(0|\+27)[6-8][0-9]{8}$/;
            if (!phoneRegex.test(phoneClean)) {
                showTopError("Invalid information: Phone must be a valid 10-digit South African cell number (e.g. 0837949050).");
                return;
            }

            const strongRegex = /^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{8,}$/;
            if (!strongRegex.test(password)) {
                showTopError("Password must be at least 8 characters with an uppercase letter, a number, and a symbol.");
                return;
            }

            if (password !== confirm) {
                showTopError("Passwords do not match. Please verify your password entry.");
                return;
            }

            const postData = new URLSearchParams({
                fullName: fullName,
                email: email,
                phone: phoneClean,
                studentType: studentType,
                password: password,
                confirmPassword: confirm
            });

            try {
                const targetUrl = getApiUrl("api/auth/register");
                const res = await fetch(targetUrl, {
                    method: "POST",
                    headers: { "Content-Type": "application/x-www-form-urlencoded" },
                    body: postData
                });

                const rawText = await res.text();
                let data = {};
                try {
                    data = JSON.parse(rawText);
                } catch (parseErr) {
                    const cleanMsg = rawText.replace(/<[^>]*>?/gm, ' ').replace(/\s+/g, ' ').trim();
                    showTopError(cleanMsg ? cleanMsg.substring(0, 140) : "Database error: Empty response from server.");
                    return;
                }

                if (data.success) {
                    sessionStorage.setItem("userEmail", email);
                    sessionStorage.setItem("userName", fullName);
                    window.location.href = "wf04_login.html?registered=true";
                } else {
                    showTopError(data.message || "Registration rejected by database.");
                }
            } catch (err) {
                console.error("Registration request failed:", err);
                showTopError("Could not connect to database server. Please ensure Java DB is started.");
            }
        });
    }

    const loginForm = document.getElementById("loginAccessForm");
    if (loginForm) {
        loginForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            if (!validateFormAndBounce(loginForm)) return;

            const email = (document.getElementById("loginEmail")?.value || loginForm.elements["email"]?.value || "").trim().toLowerCase();
            const password = document.getElementById("loginPassword")?.value || loginForm.elements["password"]?.value || "";

            if (!email && !password) {
                showTopError("Missing information: Please enter your email and password.");
                return;
            }
            if (!email) {
                showTopError("Missing information: Please enter your registered email address.");
                return;
            }
            if (!password) {
                showTopError("Missing information: Please enter your password.");
                return;
            }

            const postData = new URLSearchParams({ email, password });

            try {
                const res = await fetch(getApiUrl("api/auth/login"), {
                    method: "POST",
                    headers: { "Content-Type": "application/x-www-form-urlencoded" },
                    body: postData
                });

                const raw = await res.text();
                let data = {};
                try {
                    data = JSON.parse(raw);
                } catch (err) {
                    const cleanMsg = raw.replace(/<[^>]*>?/gm, ' ').replace(/\s+/g, ' ').trim();
                    showTopError(cleanMsg ? cleanMsg.substring(0, 140) : "Server error during authentication.");
                    return;
                }

                if (data.success) {
                    sessionStorage.setItem("userId", data.userId);
                    sessionStorage.setItem("userName", data.userName);
                    window.location.href = data.profileCompleted ? "wf11_discover.html" : "wf05_bursary_quest.html";
                } else {
                    showTopError(data.message || "Incorrect password.");
                }
            } catch (err) {
                showTopError("Server communication error during authentication.");
            }
        });
    }

    const step1Form = document.getElementById("step1IdentityForm");
    if (step1Form) {
        step1Form.addEventListener("submit", async (e) => {
            e.preventDefault();
            if (!validateFormAndBounce(step1Form)) return;
            const prov = document.getElementById("selectedProvinceInput")?.value;
            if (!prov) {
                showTopError("Missing information: Please choose your province in South Africa.");
                return;
            }

            const postData = new URLSearchParams(new FormData(step1Form));
            try {
                const res = await fetch(getApiUrl("api/quest/step1-identity"), { method: "POST", body: postData });
                if (res.ok) {
                    window.location.href = "wf07_step2_academic.html";
                } else {
                    showTopError("Failed to save your details. Please try again.");
                }
            } catch (err) {
                showTopError("Server error saving your details.");
            }
        });
    }

    const step2Form = document.getElementById("step2AcademicForm");
    if (step2Form) {
        step2Form.addEventListener("submit", async (e) => {
            e.preventDefault();
            const grade = document.getElementById("selectedGradeInput")?.value;
            const field = document.getElementById("selectedFieldInput")?.value;

            if (!grade) {
                showTopError("Missing information: Please select your Current Grade or Academic Year.");
                return;
            }
            if (!field) {
                showTopError("Missing information: Please select your Field of Study.");
                return;
            }

            // fieldOfStudy is needed again later (calculate-dna and the bursary
            // match count), and there's no server session state carrying it
            // between these separately-navigated quest pages, so it's kept in
            // sessionStorage for this browser tab only.
            sessionStorage.setItem("questFieldOfStudy", field);

            const postData = new URLSearchParams(new FormData(step2Form));
            try {
                const res = await fetch(getApiUrl("api/quest/step2-academic"), { method: "POST", body: postData });
                if (res.ok) {
                    window.location.href = "wf08_step3_dna.html";
                } else {
                    showTopError("Failed to save your academic details. Please try again.");
                }
            } catch (err) {
                showTopError("Server error saving your academic details.");
            }
        });
    }

    if (page === "step3") {
        const addRowBtn = document.getElementById("addNewSubjectRowBtn");
        if (addRowBtn) addRowBtn.addEventListener("click", appendDynamicMarkRow);

        const calcBtn = document.getElementById("calculateDnaResultsBtn");
        if (calcBtn) calcBtn.addEventListener("click", calculateAcademicResults);

        const proceedBtn = document.getElementById("proceedStep4Btn");
        if (proceedBtn) {
            proceedBtn.addEventListener("click", async (e) => {
                e.preventDefault();
                const avg = calculateAcademicResults();
                if (avg <= 0) return;

                const rows = document.querySelectorAll("#dynamicMarksRowsList .mark-row-item");
                const marks = [];
                rows.forEach(r => {
                    const sub = r.querySelector(".subject-input")?.value.trim();
                    const score = parseFloat(r.querySelector(".score-input")?.value);
                    if (sub && !isNaN(score)) marks.push({ subject: sub, score: score });
                });

                sessionStorage.setItem("questAcademicAverage", String(avg));

                try {
                    await fetch(getApiUrl("api/quest/step3-marks"), {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ marks: marks, average: avg })
                    });
                } catch (err) {
                    console.warn("Could not save subject marks, continuing anyway:", err);
                }

                window.location.href = "wf09_step4_household.html";
            });
        }
    }

    if (page === "step4") {
        const incomeCards = document.querySelectorAll("#incomeCardsFeed .card-radio");
        const nsfasWarning = document.getElementById("nsfasDisqualifiedAlert");

        incomeCards.forEach(card => {
            card.addEventListener("click", () => {
                incomeCards.forEach(c => c.classList.remove("selected"));
                card.classList.add("selected");
                const bracket = card.dataset.bracket;
                const bracketInput = document.getElementById("selectedIncomeBracket");
                if (bracketInput) bracketInput.value = bracket;

                if (nsfasWarning) {
                    if (bracket === "R350K - R600K" || bracket === "R600K +") {
                        nsfasWarning.classList.remove("is-hidden");
                    } else {
                        nsfasWarning.classList.add("is-hidden");
                    }
                }
            });
        });

        const form = document.getElementById("step4HouseholdForm");
        if (form) {
            form.addEventListener("submit", async (e) => {
                e.preventDefault();
                const bracket = document.getElementById("selectedIncomeBracket")?.value;
                if (!bracket) {
                    showTopError("Missing information: Please choose your household income bracket.");
                    return;
                }

                const average = sessionStorage.getItem("questAcademicAverage") || "0";
                const field = sessionStorage.getItem("questFieldOfStudy") || "";

                const postData = new URLSearchParams({
                    averageScore: average,
                    incomeBracket: bracket,
                    fieldOfStudy: field
                });

                try {
                    const res = await fetch(getApiUrl("api/quest/calculate-dna"), { method: "POST", body: postData });
                    const data = await res.json();
                    if (res.ok && data.success) {
                        sessionStorage.setItem("questResults", JSON.stringify(data));
                        window.location.href = "wf10_step5_results.html";
                    } else {
                        showTopError(data.message || "Failed to calculate your results. Please try again.");
                    }
                } catch (err) {
                    showTopError("Server error calculating your DNA results.");
                }
            });
        }
    }

    if (page === "step5") {
        const raw = sessionStorage.getItem("questResults");
        const results = raw ? JSON.parse(raw) : null;

        const matchesTextElem = document.getElementById("dnaMatchesText");
        const dnaPctElem = document.getElementById("kpiDnaPercentage");
        const matchesCountElem = document.getElementById("kpiMatchesCount");
        const valueElem = document.getElementById("kpiPotentialValue");
        const academicElem = document.getElementById("kpiAcademicAverage");

        if (results) {
            if (matchesTextElem) matchesTextElem.textContent = `${results.matchedBursariesCount} bursaries`;
            if (dnaPctElem) dnaPctElem.textContent = `${results.dnaScore}%`;
            if (matchesCountElem) matchesCountElem.textContent = results.matchedBursariesCount;
            if (valueElem) valueElem.textContent = results.potentialFundingValue;
            if (academicElem) academicElem.textContent = `${results.academicAverage}%`;
        } else {
            // No sessionStorage results (e.g. page refreshed, or reached
            // directly) - show honest zero/placeholder state rather than
            // any invented numbers.
            if (matchesTextElem) matchesTextElem.textContent = "0 bursaries";
            if (dnaPctElem) dnaPctElem.textContent = "--";
            if (matchesCountElem) matchesCountElem.textContent = "0";
            if (valueElem) valueElem.textContent = "R0";
            if (academicElem) academicElem.textContent = "--";
        }
    }

    if (page === "discovery") {
        searchLiveBursaries();
        const searchBox = document.getElementById("bursarySearchBox");
        if (searchBox) searchBox.addEventListener("input", searchLiveBursaries);
    }

    if (page === "bursary-details") {
        const applyBtn = document.getElementById("submitApplicationActionBtn");
        if (applyBtn) {
            applyBtn.addEventListener("click", async () => {
                const bursaryId = document.getElementById("activeBursaryId")?.value;
                try {
                    const res = await fetch(getApiUrl("api/bursaries/apply"), {
                        method: "POST",
                        headers: { "Content-Type": "application/x-www-form-urlencoded" },
                        body: new URLSearchParams({ bursaryId })
                    });
                    const result = await res.json();
                    if (res.status === 412) {
                        showTopError("Application Blocked: Missing required documents in your Document Vault. Upload ID and income proof before applying.", "wf13_vault_main.html");
                    } else if (res.ok) {
                        alert(result.message || "Application successfully submitted!");
                        window.location.href = "wf11_discover.html";
                    } else {
                        showTopError(result.message || "Application submission failed.");
                    }
                } catch (err) {
                    showTopError("Error submitting bursary application.");
                }
            });
        }
    }

    if (page === "vault") {
        loadVaultDashboard();
    }

    if (page === "upload") {
        const dropzone = document.getElementById("dropzoneArea");
        const fileInput = document.getElementById("vaultFileInput");
        const fileNameDisplay = document.getElementById("selectedFileNameDisplay");
        const form = document.getElementById("vaultUploadForm");

        if (dropzone && fileInput) {
            dropzone.addEventListener("click", () => fileInput.click());
            fileInput.addEventListener("change", (e) => {
                const file = e.target.files[0];
                if (file && fileNameDisplay) {
                    fileNameDisplay.textContent = `Selected: ${file.name} (${(file.size / 1024).toFixed(1)} KB)`;
                    fileNameDisplay.classList.remove("is-hidden");
                }
            });
        }

        if (form) {
            form.addEventListener("submit", async (e) => {
                e.preventDefault();
                const file = fileInput.files[0];
                if (!file) {
                    showTopError("Please choose a file to upload.");
                    return;
                }
                if (file.size > 10 * 1024 * 1024) {
                    showTopError("File exceeds the maximum allowable limit of 10 MB.");
                    return;
                }

                const docType = document.getElementById("docTypeSelect")?.value || "Certified ID";
                const formData = new FormData();
                formData.append("file", file);
                formData.append("documentType", docType);

                try {
                    const res = await fetch(getApiUrl("api/documents"), { method: "POST", body: formData });
                    const result = await res.json();
                    if (res.ok) {
                        window.location.href = "wf13_vault_main.html";
                    } else {
                        showTopError(result.message || "Failed to upload document.");
                    }
                } catch (err) {
                    showTopError("Server communication error while uploading file.");
                }
            });
        }
    }

    if (page === "add-transaction") {
        const toggleExpBtn = document.getElementById("toggleExpenseBtn");
        const toggleIncBtn = document.getElementById("toggleIncomeBtn");
        const txTypeInput = document.getElementById("txTypeInput");
        const form = document.getElementById("transactionEntryForm");

        // "+ Add Balance" tabs link here with ?type=income so the form opens
        // straight onto the Income side instead of defaulting to Expense.
        const params = new URLSearchParams(window.location.search);

        if (toggleExpBtn && toggleIncBtn) {
            toggleExpBtn.addEventListener("click", () => {
                if (txTypeInput) txTypeInput.value = "Expense";
                toggleExpBtn.classList.add("active");
                toggleIncBtn.classList.remove("active");
            });
            toggleIncBtn.addEventListener("click", () => {
                if (txTypeInput) txTypeInput.value = "Income";
                toggleIncBtn.classList.add("active");
                toggleExpBtn.classList.remove("active");
            });

            if (params.get("type") === "income") {
                toggleIncBtn.click();
            }
        }

        if (form) {
            form.addEventListener("submit", async (e) => {
                e.preventDefault();
                if (!validateFormAndBounce(form)) return;

                const postData = new URLSearchParams(new FormData(form));
                try {
                    const res = await fetch(getApiUrl("api/wallet/transaction"), { method: "POST", body: postData });
                    const data = await res.json();
                    if (res.ok) {
                        window.location.href = "wf19_wallet_overview.html";
                    } else {
                        showTopError(data.message || "Failed to log transaction.");
                    }
                } catch (err) {
                    showTopError("Server error logging transaction.");
                }
            });
        }
    }

    if (page === "add-budget") {
        const form = document.getElementById("categoryLimitForm");
        if (form) {
            form.addEventListener("submit", async (e) => {
                e.preventDefault();
                if (!validateFormAndBounce(form)) return;

                const postData = new URLSearchParams(new FormData(form));
                try {
                    const res = await fetch(getApiUrl("api/wallet/category"), { method: "POST", body: postData });
                    const data = await res.json();
                    if (res.ok) {
                        window.location.href = "wf20_wallet_spending.html";
                    } else {
                        showTopError(data.message || "Failed to add budget category.");
                    }
                } catch (err) {
                    showTopError("Server error adding budget category.");
                }
            });
        }
    }

    if (page === "wallet-overview" || page === "wallet-spending" || page === "wallet-trends") {
        loadWalletData();
    }

    if (page === "calendar") {
        renderLiveCalendar(activeCalendarDate);

        const prevBtn = document.getElementById("prevMonthBtn");
        const nextBtn = document.getElementById("nextMonthBtn");

        if (prevBtn) {
            prevBtn.addEventListener("click", () => {
                activeCalendarDate.setMonth(activeCalendarDate.getMonth() - 1);
                renderLiveCalendar(activeCalendarDate);
            });
        }
        if (nextBtn) {
            nextBtn.addEventListener("click", () => {
                activeCalendarDate.setMonth(activeCalendarDate.getMonth() + 1);
                renderLiveCalendar(activeCalendarDate);
            });
        }
    }

    if (page === "add-event") {
        const form = document.getElementById("calendarEventForm");
        if (form) {
            form.addEventListener("submit", async (e) => {
                e.preventDefault();
                if (!validateFormAndBounce(form)) return;

                const postData = new URLSearchParams(new FormData(form));
                try {
                    const res = await fetch(getApiUrl("api/calendar"), { method: "POST", body: postData });
                    const result = await res.json();
                    if (res.ok) {
                        window.location.href = "wf22_calendar_main.html";
                    } else {
                        showTopError(result.message || "Failed to schedule event.");
                    }
                } catch (err) {
                    showTopError("Communication error with calendar service.");
                }
            });
        }
    }

    if (page === "learn-curriculum") {
        (async () => {
            try {
                const res = await fetch(getApiUrl("api/learn"));
                if (res.ok) {
                    const lessons = await res.json();
                    lessons.forEach(l => {
                        const checkCircle = document.querySelector(`.status-check-circle[data-check-id="${l.lessonId}"]`);
                        if (checkCircle && l.completed) {
                            checkCircle.classList.add("completed");
                        }
                    });
                }
            } catch (err) {
                console.warn("Could not retrieve real-time lesson progress:", err);
            }
        })();
    }

    if (page === "course-content") {
        const completeBtn = document.getElementById("markLessonCompleteBtn");
        if (completeBtn) {
            completeBtn.addEventListener("click", async (e) => {
                e.preventDefault();
                const lessonId = document.getElementById("activeLessonId")?.value;
                try {
                    const res = await fetch(getApiUrl("api/learn/complete"), {
                        method: "POST",
                        headers: { "Content-Type": "application/x-www-form-urlencoded" },
                        body: new URLSearchParams({ lessonId })
                    });
                    if (res.ok) {
                        alert("Lesson Complete! +20 XP awarded to your student profile.");
                        window.location.href = "wf25_learn_curriculum.html";
                    }
                } catch (err) {
                    window.location.href = "wf25_learn_curriculum.html";
                }
            });
        }
    }

    if (page === "notification") {
        (async () => {
            const feed = document.getElementById("notifListFeed");
            if (!feed) return;
            try {
                const res = await fetch(getApiUrl("api/notifications"));
                if (res.ok) {
                    const list = await res.json();
                    feed.textContent = "";

                    if (!Array.isArray(list) || list.length === 0) {
                        const emptyCard = document.createElement("div");
                        emptyCard.className = "card text-center";
                        emptyCard.textContent = "No pending notifications. All systems up to date!";
                        feed.appendChild(emptyCard);
                        return;
                    }

                    list.forEach(n => {
                        const link = document.createElement("a");
                        link.href = n.category === "Wallet" ? "wf19_wallet_overview.html" : (n.category === "Application" ? "wf13_vault_main.html" : "wf11_discover.html");
                        link.className = `notification-card unread ${n.category === "Wallet" ? "card-red" : (n.category === "Application" ? "card-amber" : "card-blue")}`;

                        const topRow = document.createElement("div");
                        topRow.className = "item-header-row";

                        const titleStrong = document.createElement("strong");
                        titleStrong.textContent = n.title;

                        const timeSpan = document.createElement("span");
                        timeSpan.className = "deadline-callout";
                        timeSpan.textContent = n.timeLabel || "Active Alert";

                        topRow.appendChild(titleStrong);
                        topRow.appendChild(timeSpan);

                        const msgPara = document.createElement("p");
                        msgPara.className = "card-feature-desc";
                        msgPara.textContent = n.message;

                        const catBadge = document.createElement("span");
                        catBadge.className = "badge badge-purple";
                        catBadge.textContent = n.category;

                        const dot = document.createElement("span");
                        dot.className = "notification-unread-dot";

                        link.appendChild(topRow);
                        link.appendChild(msgPara);
                        link.appendChild(catBadge);
                        link.appendChild(dot);
                        feed.appendChild(link);
                    });
                }
            } catch (e) {
                console.error("Notifications fetch error:", e);
            }
        })();
    }
});