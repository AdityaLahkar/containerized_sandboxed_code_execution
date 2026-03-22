document.addEventListener('DOMContentLoaded', () => {
    // 1. Grab HTML elements to easily interact with them
    const runBtn = document.getElementById('run-btn');
    const languageSelect = document.getElementById('language-select');
    const codeEditor = document.getElementById('code-editor');
    const terminalOutput = document.getElementById('terminal-output');

    // 2. Pre-fill the editor based on the selected language
    // These keys match the `LanguageStrategy.getLanguageName()` strings perfectly!
    const templates = {
        'c': '#include <stdio.h>\n\nint main() {\n    printf("Hello from C Sandbox!\\n");\n    return 0;\n}',
        'cpp': '#include <iostream>\n\nint main() {\n    std::cout << "Hello from C++ Sandbox!" << std::endl;\n    return 0;\n}',
        'java': 'public class Main {\n    public static void main(String[] args) {\n        System.out.println("Hello from Java Sandbox!");\n    }\n}',
        'python': 'print("Hello from Python Sandbox!")'
    };

    // Set initial text on page load
    codeEditor.value = templates[languageSelect.value];

    // Listen for dropdown changes to swap the template
    languageSelect.addEventListener('change', (e) => {
        codeEditor.value = templates[e.target.value];
    });

    // 3. Tab Key Support inside the Textarea
    // By default, pressing 'Tab' jumps out of the textarea. This intercepts it and inserts 4 spaces.
    codeEditor.addEventListener('keydown', function(e) {
        if (e.key === 'Tab') {
            e.preventDefault();
            const start = this.selectionStart;
            const end = this.selectionEnd;
            this.value = this.value.substring(0, start) + "    " + this.value.substring(end);
            this.selectionStart = this.selectionEnd = start + 4;
        }
    });

    // 4. Submit the Request to our Spring Boot Backend
    runBtn.addEventListener('click', async () => {
        const language = languageSelect.value;
        const code = codeEditor.value;

        if (!code.trim()) return;

        // Change UI state to let user know it's working
        runBtn.classList.add('loading');
        runBtn.innerHTML = 'Executing...';
        terminalOutput.innerHTML = '<span class="welcome-msg">Running...</span>';

        try {
            // Because our frontend and backend run on the exact same server (localhost:8080)
            // We just hit the relative path `/api/run` directly
            const response = await fetch('/api/run', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                // Pack the data perfectly into our CodeRequest object schema
                body: JSON.stringify({ language, code })
            });

            const data = await response.json();

            // Check if Spring threw a 400 or 500 error (our GlobalExceptionHandler)
            if (!response.ok) {
                terminalOutput.innerHTML = `<span class="error-msg">Server Infrastructure Error: ${data.error}</span>`;
                return;
            }

            // 5. Route the display color based on execution status
            if (data.status === 'success') {
                terminalOutput.innerHTML = `<span class="success-msg">[Exited with code 0]</span>\n\n${escapeHTML(data.output)}`;
            } else if (data.status === 'timeout') {
                terminalOutput.innerHTML = `<span class="error-msg">[Process Terminated]</span>\n${data.error}`;
            } else {
                // Compilation error / Runtime Exception
                terminalOutput.innerHTML = `<span class="error-msg">[Exited with non-zero status]</span>\n\n${escapeHTML(data.error)}`;
            }

        } catch (err) {
            terminalOutput.innerHTML = `<span class="error-msg">Network Error: Could not connect to the Backend.</span>`;
        } finally {
            // Revert button back to normal state
            runBtn.classList.remove('loading');
            runBtn.innerHTML = `
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M5 3l14 9-14 9V3z" fill="currentColor"/>
                </svg>
                Run Code
            `;
        }
    });

    // Utility: Prevents users from printing XSS `<script>` tags via C output to hack our frontend.
    function escapeHTML(str) {
        if (!str) return '';
        return str.replace(/[&<>'"]/g, 
            tag => ({
                '&': '&amp;',
                '<': '&lt;',
                '>': '&gt;',
                "'": '&#39;',
                '"': '&quot;'
            }[tag])
        );
    }
});
