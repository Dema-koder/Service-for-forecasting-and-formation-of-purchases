from sklearn.base import BaseEstimator, TransformerMixin


# Custom Transformer for splitting codes into separate columns
class CodeSplitter(BaseEstimator, TransformerMixin):
    def __init__(self, column, max_depth=9):
        self.column = column
        self.max_depth = max_depth

    def fit(self, X, y=None):
        return self

    def transform(self, X):
        # Split the codes into separate columns
        split_columns = X[self.column].str.split('.', expand=True)

        # Dynamically assign column names based on the maximum depth
        for i in range(self.max_depth - split_columns.shape[1]):
            split_columns[f'level{split_columns.shape[1] + i + 1}'] = -1.0

        split_columns.columns = [f'level{i+1}' for i in range(self.max_depth)]
        return split_columns

