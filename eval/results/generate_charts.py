"""
Generate 4 evaluation charts from eval-results.csv.
Output: eval/results/charts/ directory
"""
import csv
import os
import sys
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.ticker as mticker
import numpy as np

# --- Config ---
CSV_PATH = os.path.join(os.path.dirname(__file__), 'eval-results.csv')
OUT_DIR = os.path.join(os.path.dirname(__file__), 'charts')
os.makedirs(OUT_DIR, exist_ok=True)

# Color scheme — consistent across all charts
COLORS = {
    'baseline':    '#94a3b8',  # slate gray — bare/minimal
    'gstack':       '#22c55e',  # green — winner
    'openspec':     '#3b82f6',  # blue
    'superpowers':  '#f59e0b',  # amber
}
PROVIDER_ORDER = ['baseline', 'gstack', 'openspec', 'superpowers']
PROVIDER_LABELS = {
    'baseline': 'Baseline\n(裸Agent)',
    'gstack': 'Gstack',
    'openspec': 'OpenSpec',
    'superpowers': 'Superpowers',
}
PROVIDER_LABELS_SHORT = {
    'baseline': 'Baseline',
    'gstack': 'Gstack',
    'openspec': 'OpenSpec',
    'superpowers': 'Superpowers',
}

# Configure Chinese font
plt.rcParams['font.sans-serif'] = ['Microsoft YaHei', 'SimHei', 'Noto Sans CJK SC', 'WenQuanYi Micro Hei', 'DejaVu Sans']
plt.rcParams['axes.unicode_minus'] = False

# --- Load data ---
rows = []
with open(CSV_PATH, 'r', encoding='utf-8') as f:
    reader = csv.DictReader(f)
    for r in reader:
        r['score'] = int(r['score'])
        r['duration_sec'] = int(r['duration_sec'])
        r['token_in'] = int(r['token_in'])
        r['token_out'] = int(r['token_out'])
        r['token_total'] = r['token_in'] + r['token_out']
        rows.append(r)

TASKS = sorted(set(r['task_id'] for r in rows))
PROVIDERS = [p for p in PROVIDER_ORDER if p in set(r['provider'] for r in rows)]

# Helper: provider → data list in task order
def by_task(field):
    return {p: [next((r[field] for r in rows if r['task_id']==t and r['provider']==p), 0) for t in TASKS] for p in PROVIDERS}

TASK_LABELS = [t.replace('-', '\n', 1) for t in TASKS]  # "01\nvalidation" for compact x-axis
TASK_LABELS_SHORT = [t[:2] for t in TASKS]  # "①", "②" ... but data uses 01-12, map manually
TASK_NUMS = ['①','②','③','④','⑤','⑥','⑦','⑧','⑨','⑩','11','12']

# ============================================================
# Chart 1: Total Score Bar Chart (4 providers side-by-side)
# ============================================================
fig, ax = plt.subplots(figsize=(10, 6))

totals = {p: sum(r['score'] for r in rows if r['provider']==p) for p in PROVIDERS}
x = np.arange(len(PROVIDERS))
bars = ax.bar(x, [totals[p] for p in PROVIDERS],
              color=[COLORS[p] for p in PROVIDERS],
              edgecolor='white', linewidth=1.2, width=0.55)

# Value labels on bars
for bar, p in zip(bars, PROVIDERS):
    ax.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 6,
            str(totals[p]), ha='center', va='bottom', fontsize=18, fontweight='bold',
            color=COLORS[p])

ax.set_xticks(x)
ax.set_xticklabels([PROVIDER_LABELS_SHORT[p] for p in PROVIDERS], fontsize=13)
ax.set_ylabel('总分', fontsize=13)
ax.set_title('12 任务总分对比', fontsize=16, fontweight='bold', pad=15)
ax.set_ylim(0, max(totals.values()) + 70)
ax.spines['top'].set_visible(False)
ax.spines['right'].set_visible(False)
ax.yaxis.set_major_locator(mticker.MultipleLocator(100))
ax.grid(axis='y', alpha=0.3, linestyle='--')

# Highlight Gstack
bars[1].set_edgecolor('#166534')
bars[1].set_linewidth(2.5)

# Add gap annotation between Gstack and Superpowers/Baseline
ax.annotate('', xy=(3, 1045), xytext=(1, 1090),
            arrowprops=dict(arrowstyle='<->', color='#ef4444', lw=1.5, shrinkA=0, shrinkB=0))
ax.text(2, 1070, '差距 45 分', ha='center', fontsize=10, color='#ef4444', fontweight='bold')

fig.tight_layout()
fig.savefig(os.path.join(OUT_DIR, '01-total-scores.png'), dpi=150, bbox_inches='tight')
plt.close(fig)
print('[OK] 01-total-scores.png')

# ============================================================
# Chart 2: Score Trend Across Tasks (line chart)
# ============================================================
fig, ax = plt.subplots(figsize=(14, 6))

scores = by_task('score')
x = np.arange(len(TASKS))

for p in PROVIDERS:
    ax.plot(x, scores[p], marker='o', markersize=8, linewidth=2.2,
            color=COLORS[p], label=PROVIDER_LABELS_SHORT[p], zorder=5,
            markeredgecolor='white', markeredgewidth=1)

# Fill the gap between Gstack and others
ax.fill_between(x, scores['gstack'], scores['baseline'], alpha=0.08, color=COLORS['gstack'])

ax.set_xticks(x)
ax.set_xticklabels(TASK_NUMS, fontsize=12)
ax.set_ylabel('得分', fontsize=13)
ax.set_title('12 任务得分趋势（L1 → L4）', fontsize=16, fontweight='bold', pad=15)
ax.legend(fontsize=11, framealpha=0.9, edgecolor='#ddd')
ax.set_ylim(55, 105)
ax.spines['top'].set_visible(False)
ax.spines['right'].set_visible(False)
ax.grid(axis='y', alpha=0.3, linestyle='--')
ax.grid(axis='x', alpha=0.3, linestyle='--')

# Difficulty zone labels
ax.axvspan(-0.5, 1.5, alpha=0.04, color='green', label='_')
ax.axvspan(1.5, 5.5, alpha=0.04, color='blue', label='_')
ax.axvspan(5.5, 8.5, alpha=0.04, color='orange', label='_')
ax.axvspan(8.5, 11.5, alpha=0.04, color='red', label='_')
ax.text(0.5, 57, 'L1', ha='center', fontsize=9, color='#888')
ax.text(3.5, 57, 'L2', ha='center', fontsize=9, color='#888')
ax.text(7.0, 57, 'L3', ha='center', fontsize=9, color='#888')
ax.text(10.0, 57, 'L4', ha='center', fontsize=9, color='#888')

# Annotate key divergence points
# Task 10: N+1 fix — Gstack 90 vs Baseline 65
ax.annotate('N+1修复\nGstack +25', xy=(9, 90), xytext=(9.5, 97),
            fontsize=8, ha='center', color='#166534', fontweight='bold',
            arrowprops=dict(arrowstyle='->', color='#22c55e', lw=1.2))

fig.tight_layout()
fig.savefig(os.path.join(OUT_DIR, '02-score-trend.png'), dpi=150, bbox_inches='tight')
plt.close(fig)
print('[OK] 02-score-trend.png')

# ============================================================
# Chart 3: Token Efficiency (scatter: total tokens vs score)
# ============================================================
fig, ax = plt.subplots(figsize=(10, 7))

for p in PROVIDERS:
    xs = [r['token_total'] for r in rows if r['provider'] == p]
    ys = [r['score'] for r in rows if r['provider'] == p]
    sizes = [r['duration_sec'] / 3 for r in rows if r['provider'] == p]  # bubble size ~ duration
    ax.scatter(xs, ys, s=sizes, c=COLORS[p], alpha=0.7, edgecolors='white',
               linewidth=0.8, label=PROVIDER_LABELS_SHORT[p], zorder=5)

ax.set_xlabel('Token 总量（输入 + 输出）', fontsize=13)
ax.set_ylabel('得分', fontsize=13)
ax.set_title('Token 效率：消耗 vs 得分（气泡大小 = 耗时）', fontsize=16, fontweight='bold', pad=15)
ax.legend(fontsize=11, framealpha=0.9, edgecolor='#ddd')
ax.spines['top'].set_visible(False)
ax.spines['right'].set_visible(False)
ax.grid(alpha=0.3, linestyle='--')
ax.set_ylim(55, 105)

# Add efficiency quadrant lines
median_tokens = np.median([r['token_total'] for r in rows])
median_score = np.median([r['score'] for r in rows])
ax.axvline(median_tokens, color='#ccc', linestyle=':', alpha=0.5)
ax.axhline(median_score, color='#ccc', linestyle=':', alpha=0.5)
ax.text(median_tokens + 2000, 102, '高消耗\n高得分', fontsize=8, color='#aaa', ha='center')
ax.text(median_tokens + 2000, 58, '高消耗\n低得分', fontsize=8, color='#aaa', ha='center')
ax.text(25000, 102, '低消耗\n高得分', fontsize=8, color='#aaa', ha='center')
ax.text(25000, 58, '低消耗\n低得分', fontsize=8, color='#aaa', ha='center')

fig.tight_layout()
fig.savefig(os.path.join(OUT_DIR, '03-token-efficiency.png'), dpi=150, bbox_inches='tight')
plt.close(fig)
print('[OK] 03-token-efficiency.png')

# ============================================================
# Chart 4: Score Heatmap (12 tasks × 4 providers)
# ============================================================
fig, ax = plt.subplots(figsize=(12, 5.5))

# Build matrix: rows=tasks (reversed for top-to-bottom), cols=providers
matrix = []
for t in TASKS:
    row_list = []
    for p in PROVIDERS:
        val = next((r['score'] for r in rows if r['task_id']==t and r['provider']==p), 0)
        row_list.append(val)
    matrix.append(row_list)
matrix = np.array(matrix)

im = ax.imshow(matrix.T, cmap='RdYlGn', aspect='auto', vmin=60, vmax=100)

# Axis labels
ax.set_xticks(np.arange(len(TASKS)))
ax.set_xticklabels([f'{TASK_NUMS[i]} {TASKS[i][:12]}' for i in range(len(TASKS))], fontsize=9)
ax.set_yticks(np.arange(len(PROVIDERS)))
ax.set_yticklabels([PROVIDER_LABELS_SHORT[p] for p in PROVIDERS], fontsize=12)

# Value text in each cell
for i in range(len(TASKS)):
    for j in range(len(PROVIDERS)):
        val = matrix[i, j]
        text_color = 'white' if val <= 70 else ('#333' if val >= 90 else '#222')
        ax.text(i, j, str(val), ha='center', va='center', fontsize=13,
                fontweight='bold', color=text_color)

# Rotate x labels
plt.setp(ax.get_xticklabels(), rotation=30, ha='right', rotation_mode='anchor')

# Colorbar
cbar = fig.colorbar(im, ax=ax, shrink=0.85, pad=0.02)
cbar.set_label('得分', fontsize=12)

ax.set_title('12 任务 × 4 Provider 得分热力图', fontsize=16, fontweight='bold', pad=15)

fig.tight_layout()
fig.savefig(os.path.join(OUT_DIR, '04-score-heatmap.png'), dpi=150, bbox_inches='tight')
plt.close(fig)
print('[OK] 04-score-heatmap.png')

# ============================================================
# Summary
# ============================================================
print(f'\nGenerated 4 charts -> {OUT_DIR}/')
for f in sorted(os.listdir(OUT_DIR)):
    size_kb = os.path.getsize(os.path.join(OUT_DIR, f)) / 1024
    print(f'  {f} ({size_kb:.0f} KB)')
