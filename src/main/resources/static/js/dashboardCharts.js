(function () {
    function carregarBibliotecaChart() {
        if (window.Chart) return Promise.resolve();
        if (window.criareCarregamentoChart) return window.criareCarregamentoChart;

        window.criareCarregamentoChart = new Promise(function (resolve, reject) {
            const script = document.createElement('script');
            script.src = 'https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js';
            script.onload = resolve;
            script.onerror = reject;
            document.head.appendChild(script);
        });
        return window.criareCarregamentoChart;
    }

    function destruirGrafico(id) {
        const canvas = document.getElementById(id);
        if (canvas && window.Chart) {
            const grafico = Chart.getChart(canvas);
            if (grafico) grafico.destroy();
        }
    }

    function destruirGraficosDashboard() {
        destruirGrafico('graficoLucroMensal');
        destruirGrafico('graficoPlataformas');
    }

    function iniciarGraficosDashboard() {
        const dadosElemento = document.getElementById('dashboard-chart-data');
        if (!dadosElemento) return;

        let dados;
        try {
            dados = JSON.parse(dadosElemento.textContent);
        } catch (erro) {
            console.error('Não foi possível ler os dados dos gráficos da dashboard.', erro);
            return;
        }

        carregarBibliotecaChart()
            .then(function () {
                requestAnimationFrame(function () {
                    if (document.getElementById('dashboard-chart-data')) {
                        desenharGraficosDashboard(dados);
                    }
                });
            })
            .catch(function (erro) {
                console.error('Não foi possível carregar os gráficos da dashboard.', erro);
            });
    }

    function desenharGraficosDashboard(dados) {
        destruirGraficosDashboard();
        const estiloTema = getComputedStyle(document.documentElement);
        const corCss = nome => estiloTema.getPropertyValue(nome).trim();

        const canvasMensal = document.getElementById('graficoLucroMensal');
        if (canvasMensal) {
            new Chart(canvasMensal, {
                type: 'line',
                data: {
                    labels: dados.rotulosLucro,
                    datasets: [{
                        data: dados.valoresLucro,
                        borderColor: corCss('--dashboard-tiktok'),
                        backgroundColor: corCss('--dashboard-chart-fill'),
                        fill: true,
                        tension: 0.35,
                        borderWidth: 2,
                        // Mantém os valores diários, mas destaca somente os dias com movimento.
                        pointRadius: contexto => Number(contexto.raw) !== 0 ? 4 : 0,
                        pointHoverRadius: contexto => Number(contexto.raw) !== 0 ? 5 : 0,
                        pointBackgroundColor: corCss('--dashboard-tiktok'),
                        pointBorderColor: '#ffffff',
                        pointBorderWidth: 2
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { display: false } },
                    scales: {
                        x: {
                            grid: { display: false },
                            ticks: {
                                color: corCss('--dashboard-chart-text'),
                                font: { size: 11 },
                                autoSkip: true,
                                maxTicksLimit: dados.rotulosLucro.length <= 31 ? 6
                                    : dados.rotulosLucro.length <= 90 ? 7 : 8
                            }
                        },
                        y: {
                            beginAtZero: true,
                            grid: { color: corCss('--dashboard-chart-grid') },
                            border: { display: false },
                            ticks: {
                                color: corCss('--dashboard-chart-text'),
                                font: { size: 11 },
                                callback: valor => new Intl.NumberFormat('pt-BR', {
                                    style: 'currency', currency: 'BRL', maximumFractionDigits: 0
                                }).format(valor)
                            }
                        }
                    }
                }
            });
        }

        const canvasPlataformas = document.getElementById('graficoPlataformas');
        if (canvasPlataformas) {
            new Chart(canvasPlataformas, {
                type: 'doughnut',
                data: {
                    labels: dados.plataformas,
                    datasets: [{
                        data: dados.lucroPlataformas,
                        backgroundColor: dados.coresPlataformas.map(function (cor) {
                            return cor.startsWith('--')
                                ? getComputedStyle(document.documentElement).getPropertyValue(cor).trim()
                                : cor;
                        }),
                        borderColor: '#ffffff',
                        borderWidth: 2
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    cutout: '66%',
                    plugins: { legend: { display: false } }
                }
            });
        }
    }

    document.addEventListener('turbo:load', iniciarGraficosDashboard);
    document.addEventListener('turbo:render', iniciarGraficosDashboard);
    document.addEventListener('turbo:before-cache', destruirGraficosDashboard);
    window.addEventListener('pageshow', iniciarGraficosDashboard);
    document.addEventListener('DOMContentLoaded', iniciarGraficosDashboard, { once: true });
})();
