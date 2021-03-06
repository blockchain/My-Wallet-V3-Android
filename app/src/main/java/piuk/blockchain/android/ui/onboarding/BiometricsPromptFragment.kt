package piuk.blockchain.android.ui.onboarding

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import piuk.blockchain.android.databinding.FragmentFingerprintPromptBinding

class BiometricsPromptFragment : Fragment() {

    private var listener: OnFragmentInteractionListener? = null

    private var _binding: FragmentFingerprintPromptBinding? = null
    private val binding: FragmentFingerprintPromptBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFingerprintPromptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonEnable.setOnClickListener { listener?.onEnableFingerprintClicked() }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    internal interface OnFragmentInteractionListener {
        fun onEnableFingerprintClicked()
    }

    companion object {
        fun newInstance(): BiometricsPromptFragment {
            return BiometricsPromptFragment()
        }
    }
}
