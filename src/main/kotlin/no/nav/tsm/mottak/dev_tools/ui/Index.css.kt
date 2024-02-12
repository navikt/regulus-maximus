import org.intellij.lang.annotations.Language

@Language("CSS")
val globalCss =
    """
html {
    font-family: sans-serif;
}

body {
    margin: 0;
}

main {
    padding: 16px;
    display: grid;
    gap: 16px;
    grid-template-columns: 1fr 1fr;
}

header {
    border-bottom: 1px solid #ccc;
    height: 68px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-left: 22px;
}

h1 {
    font-size: 1.5em;
    font-weight: bold;
    margin-bottom: 8px;
}

h2 {
    font-size: 1.2em;
    font-weight: bold;
    margin-bottom: 8px;
}

button {
    padding: 8px 16px;
    border: none;
    background-color: #007bff;
    color: white;
    cursor: pointer;
    border-radius: 4px;
    transition: background-color 0.2s;
}

header img {
    height: 58px;
    margin-right: 12px;
}

.message-poster {

}

#last-ten-messages {
    padding: 4px;
}

#last-ten-messages > ul {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.success-feedback {
    animation-name: fadeOut;
    animation-duration: 5s;
    animation-fill-mode: forwards;

    background-color: #28a745;
    color: white;
    margin: 8px;
    padding: 8px;
    border-radius: 4px;
    max-width: 65ch;
}

@keyframes fadeOut {
    0% {
        opacity: 1;
    }
    80% {
        opacity: 1;
    }
    100% {
        opacity: 0;
    }
}
    """
        .trimIndent()
